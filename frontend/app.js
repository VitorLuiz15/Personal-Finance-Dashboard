// Configurações
const API_URL = '/api/transactions';
const MARKET_RATES = { cdi: 0.1125, savings: 0.0617, ipca: 0.045 };

const CATEGORIES = {
    income: ['Salário', 'Freelance', 'Renda Extra', 'Dividendos', 'Presente', 'Outros'],
    expense: ['Mercado', 'Transporte', 'Saúde', 'Lazer', 'Moradia', 'Educação', 'Assinaturas', 'Outros'],
    investment: ['Ações', 'Tesouro Direto', 'CDB/LCI/LCA', 'Cripto', 'FIIs', 'Outros']
};

let appData = { transactions: [], cashbackRate: 0.01 };
let editingId = null; 

const formatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

// --- INICIALIZAÇÃO ---
document.addEventListener('DOMContentLoaded', async () => {
    setupNavigation();
    setupForm();
    setupChartToggle();
    setupSimulator();
    updateDate();
    await loadFromServer(); // Carrega dados do Java
    refreshUI();
});

// --- COMUNICAÇÃO COM JAVA ---
async function loadFromServer() {
    try {
        const response = await fetch(API_URL);
        const data = await response.json();
        appData.transactions = data;
        console.log("Dados carregados do CSV via Java:", data);
    } catch (err) {
        console.error("Erro ao carregar do servidor. Usando dados locais de emergência.", err);
        // Fallback para localStorage se o servidor estiver offline
        const saved = localStorage.getItem('finapp_data');
        if (saved) appData = JSON.parse(saved);
    }
}

async function saveTransactionToServer(transaction) {
    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(transaction)
        });
        return response.ok;
    } catch (err) {
        console.error("Erro ao salvar no servidor:", err);
        return false;
    }
}

// --- NAVEGAÇÃO ---
function setupNavigation() {
    const navItems = document.querySelectorAll('.sidebar nav li');
    const tabContents = document.querySelectorAll('.tab-content');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tabName = item.getAttribute('data-tab');
            navItems.forEach(i => i.classList.remove('active'));
            item.classList.add('active');
            document.getElementById('page-title').innerText = item.innerText;
            tabContents.forEach(content => {
                content.id === `tab-${tabName}` ? content.classList.remove('hidden') : content.classList.add('hidden');
            });
            if (tabName === 'dashboard') refreshUI();
            if (tabName === 'investments') runSimulation();
        });
    });
}

// --- FORMULÁRIO ---
function setupForm() {
    const form = document.getElementById('transaction-form');
    if (!form) return;

    const typeSelect = document.getElementById('t-type');
    const categorySelect = document.getElementById('t-category');
    const otherGroup = document.getElementById('other-category-group');
    const otherInput = document.getElementById('t-category-other');
    const btnSubmit = document.getElementById('btn-submit-form');
    const btnCancel = document.getElementById('btn-cancel-edit');

    const updateCategoryOptions = (selectedType, selectedCat) => {
        const options = CATEGORIES[selectedType || typeSelect.value];
        categorySelect.innerHTML = options.map(cat => `<option value="${cat}">${cat}</option>`).join('');
        if (selectedCat && !options.includes(selectedCat)) {
            categorySelect.value = 'Outros';
            otherGroup.style.display = 'block';
            otherInput.value = selectedCat;
        } else {
            otherGroup.style.display = (selectedCat === 'Outros') ? 'block' : 'none';
        }
    };

    typeSelect.addEventListener('change', () => updateCategoryOptions());
    categorySelect.addEventListener('change', () => {
        otherGroup.style.display = categorySelect.value === 'Outros' ? 'block' : 'none';
    });

    btnCancel.addEventListener('click', () => {
        editingId = null;
        form.reset();
        updateCategoryOptions();
        btnSubmit.innerText = 'Adicionar';
        btnCancel.style.display = 'none';
    });

    updateCategoryOptions();
    document.getElementById('t-date').valueAsDate = new Date();

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        let finalCategory = categorySelect.value;
        if (finalCategory === 'Outros') finalCategory = otherInput.value.trim() || 'Outros';

        const transactionData = {
            date: document.getElementById('t-date').value,
            category: finalCategory,
            type: typeSelect.value,
            amount: parseFloat(document.getElementById('t-amount').value)
        };

        const success = await saveTransactionToServer(transactionData);
        if (success) {
            await loadFromServer(); // Recarrega para pegar o novo estado do CSV
            form.reset();
            updateCategoryOptions();
            document.getElementById('t-date').valueAsDate = new Date();
            refreshUI();
            alert('Transação salva no CSV com sucesso!');
        } else {
            alert('Erro ao salvar no servidor Java.');
        }
    });
}

// (Restante das funções de cálculo e gráficos permanecem as mesmas, apenas usam o appData.transactions atualizado)

function startEdit(id) {
    const t = appData.transactions.find(t => t.id === id);
    if (!t) return;
    editingId = id;
    document.getElementById('t-date').value = t.date;
    document.getElementById('t-type').value = t.type;
    document.getElementById('t-amount').value = t.amount;
    document.getElementById('btn-submit-form').innerText = 'Salvar Alterações';
    document.getElementById('btn-cancel-edit').style.display = 'inline-block';
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function refreshUI() {
    calculateTotals();
    populateTables();
    renderCharts();
}

function calculateTotals() {
    let income = 0, expense = 0, investment = 0;
    appData.transactions.forEach(t => {
        if (t.type === 'income') income += t.amount;
        else if (t.type === 'expense') expense += t.amount;
        else if (t.type === 'investment') investment += t.amount;
    });
    document.getElementById('total-balance').innerText = formatter.format(income - expense - investment);
    document.getElementById('total-expenses').innerText = formatter.format(expense);
    document.getElementById('total-invested').innerText = formatter.format(investment);
    document.getElementById('total-cashback').innerText = formatter.format(expense * appData.cashbackRate);
}

function formatDateDisplay(dateStr) {
    if (!dateStr) return "";
    const [year, month, day] = dateStr.split('-');
    return new Date(year, month - 1, day).toLocaleDateString('pt-BR');
}

function populateTables() {
    const tables = document.querySelectorAll('table tbody');
    tables.forEach(tbody => {
        tbody.innerHTML = '';
        appData.transactions.forEach(t => {
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${formatDateDisplay(t.date)}</td><td>${t.category}</td><td class="type-${t.type}">${t.type.toUpperCase()}</td><td>${formatter.format(t.amount)}</td><td><button onclick="startEdit(${t.id})" style="background:none; border:none; color:var(--accent-blue); cursor:pointer; margin-right:8px;">✏️</button><button onclick="deleteTransaction(${t.id})" style="background:none; border:none; color:var(--accent-red); cursor:pointer;">🗑️</button></td>`;
            tbody.appendChild(tr);
        });
    });
}

function deleteTransaction(id) {
    alert("Exclusão no servidor ainda não implementada no Java. Apague manualmente no CSV por enquanto.");
}

function updateDate() {
    const el = document.getElementById('current-date');
    if (el) el.innerText = new Date().toLocaleDateString('pt-BR', { year: 'numeric', month: 'long', day: 'numeric' });
}

function setupChartToggle() {
    const selectCat = document.getElementById('category-type-select');
    if (selectCat) selectCat.addEventListener('change', () => renderCategoryChart());
    const selectEvol = document.getElementById('evolution-view-select');
    if (selectEvol) selectEvol.addEventListener('change', () => renderEvolutionChart());
}

let evolutionChart = null, categoryChart = null;
function renderCharts() {
    const tab = document.getElementById('tab-dashboard');
    if (!tab || tab.classList.contains('hidden')) return;
    renderEvolutionChart();
    renderCategoryChart();
}

function renderEvolutionChart() {
    const canvas = document.getElementById('evolutionChart');
    const container = document.getElementById('evolution-chart-container');
    const wrapper = document.getElementById('evolution-chart-wrapper');
    if (!canvas || !container) return;
    const ctx = canvas.getContext('2d');
    if (evolutionChart) evolutionChart.destroy();
    const viewMode = document.getElementById('evolution-view-select').value;
    const sorted = [...appData.transactions].sort((a, b) => new Date(a.date) - new Date(b.date));
    let labels = [], data = [], running = 0;
    if (viewMode === 'detailed') {
        sorted.forEach(t => {
            running += (t.type === 'income' ? t.amount : -t.amount);
            labels.push(formatDateDisplay(t.date));
            data.push(running);
        });
        container.style.width = `${Math.max(wrapper.offsetWidth, labels.length * 80)}px`;
    } else {
        const monthlyData = {};
        sorted.forEach(t => {
            running += (t.type === 'income' ? t.amount : -t.amount);
            const monthYear = new Date(t.date).toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' });
            monthlyData[monthYear] = running;
        });
        labels = Object.keys(monthlyData); data = Object.values(monthlyData);
        container.style.width = '100%';
    }
    evolutionChart = new Chart(ctx, { type: 'line', data: { labels, datasets: [{ label: 'Patrimônio', data, borderColor: '#2962ff', backgroundColor: 'rgba(41, 98, 255, 0.1)', fill: true, tension: 0.2 }] }, options: { responsive: true, maintainAspectRatio: false, scales: { y: { ticks: { callback: (v) => formatter.format(v) } } } } });
}

function renderCategoryChart() {
    const canvas = document.getElementById('categoryChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (categoryChart) categoryChart.destroy();
    const type = document.getElementById('category-type-select').value;
    const totals = {};
    appData.transactions.filter(t => t.type === type).forEach(t => { totals[t.category] = (totals[t.category] || 0) + t.amount; });
    categoryChart = new Chart(ctx, { type: 'doughnut', data: { labels: Object.keys(totals), datasets: [{ data: Object.values(totals), backgroundColor: ['#f84960', '#ff9f43', '#af52de', '#00c087', '#2962ff'], borderWidth: 0 }] }, options: { responsive: true, cutout: '70%' } });
}

// Simulador CDI
function setupSimulator() {
    ['sim-initial', 'sim-monthly', 'sim-months', 'sim-cdi-rate'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('input', runSimulation);
    });
}
function runSimulation() {
    const initial = parseFloat(document.getElementById('sim-initial').value) || 0;
    const monthly = parseFloat(document.getElementById('sim-monthly').value) || 0;
    const months = parseInt(document.getElementById('sim-months').value) || 0;
    const cdiPercent = parseFloat(document.getElementById('sim-cdi-rate').value) || 0;
    const annualRate = (MARKET_RATES.cdi * (cdiPercent / 100));
    const monthlyRate = Math.pow(1 + annualRate, 1/12) - 1;
    let totalBruto = initial;
    const history = [initial];
    for (let i = 1; i <= months; i++) {
        totalBruto = (totalBruto * (1 + monthlyRate)) + monthly;
        history.push(totalBruto);
    }
    document.getElementById('res-total-bruto').innerText = formatter.format(totalBruto);
    renderSimulationChart(history);
}
let simulationChart = null;
function renderSimulationChart(h) {
    const ctx = document.getElementById('simulationChart').getContext('2d');
    if (simulationChart) simulationChart.destroy();
    simulationChart = new Chart(ctx, { type: 'line', data: { labels: h.map((_, i) => `Mês ${i}`), datasets: [{ data: h, borderColor: '#00c087', fill: true }] }, options: { maintainAspectRatio: false } });
}
