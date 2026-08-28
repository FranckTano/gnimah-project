import { SERIES, CHROME, labelJourCourt, labelJourLong, montantCompact, montantComplet } from './chart-theme';

/**
 * Fabriques de graphiques Chart.js partagées par le tableau de bord et la page
 * Statistiques & KPI. Les deux écrans affichent les mêmes courbes : elles se
 * construisent donc au même endroit, options comprises.
 *
 * Chaque fabrique renvoie `{ data, options }` déjà appariés — les infobulles
 * capturent le tableau de dates de la série, il n'y a rien à re-synchroniser.
 */

export interface PointJour {
  date: string;
  valeur: number;
}

/**
 * Curseur vertical : le lecteur vise une date, jamais une ligne de 2 px.
 * Instance unique — Chart.js ne conserve aucun état dans le greffon.
 */
export const PLUGIN_CURSEUR = [{
  id: 'gnCurseur',
  afterDatasetsDraw: (chart: any) => {
    const actifs = chart.tooltip?.getActiveElements?.() || [];
    if (!actifs.length) return;
    const x = actifs[0].element.x;
    const { top, bottom } = chart.chartArea;
    const c = chart.ctx;
    c.save();
    c.beginPath();
    c.moveTo(x, top);
    c.lineTo(x, bottom);
    c.lineWidth = 1;
    c.strokeStyle = 'rgba(140,47,77,0.28)';
    c.stroke();
    c.restore();
  }
}];

function infobulle(dates: string[], format: (v: number) => string) {
  return {
    backgroundColor: '#2e2024',
    titleColor: '#e9dcdf',
    bodyColor: '#ffffff',
    titleFont: { family: 'DM Sans, sans-serif', size: 11.5, weight: '500' as const },
    bodyFont: { family: 'DM Sans, sans-serif', size: 14, weight: '600' as const },
    padding: 11,
    cornerRadius: 10,
    displayColors: false,
    caretSize: 5,
    callbacks: {
      title: (items: any[]) => labelJourLong(dates[items[0].dataIndex] || ''),
      label: (item: any) => format(item.parsed.y)
    }
  };
}

/**
 * Axe des abscisses en dates. Chart.js incline les libellés par défaut et les
 * empile dès qu'ils sont nombreux : on force l'horizontale et on laisse
 * l'auto-skip espacer les graduations.
 */
function axeDates(nb: number) {
  return {
    grid: { display: false },
    border: { display: false },
    ticks: {
      color: CHROME.encreDiscrete,
      font: { family: 'DM Sans, sans-serif', size: 11.5 },
      maxRotation: 0,
      minRotation: 0,
      autoSkip: true,
      autoSkipPadding: 14,
      maxTicksLimit: Math.min(Math.max(nb, 2), 8),
      padding: 8
    }
  };
}

function axeValeurs(format: (v: number) => string, maxTicks: number, entiers = false) {
  return {
    beginAtZero: true,
    border: { display: false },
    grid: { color: CHROME.grille, drawTicks: false },
    ticks: {
      color: CHROME.encreDiscrete,
      font: { family: 'DM Sans, sans-serif', size: 11.5 },
      maxTicksLimit: maxTicks,
      padding: 10,
      ...(entiers ? { precision: 0 } : { callback: (v: any) => format(Number(v)) })
    }
  };
}

/**
 * Courbe en aire du chiffre d'affaires par jour. Série unique : une seule
 * teinte (créneau 1), donc pas de boîte de légende — le titre de la carte dit
 * déjà ce qui est tracé.
 */
export function courbeMontantsParJour(points: PointJour[]) {
  const dates = points.map(p => p.date);
  const valeurs = points.map(p => p.valeur);
  const iMax = valeurs.indexOf(Math.max(...valeurs));
  const dernier = points.length - 1;

  return {
    dates,
    data: {
      labels: dates.map(labelJourCourt),
      datasets: [{
        label: "Chiffre d'affaires",
        data: valeurs,
        borderColor: SERIES[0],
        borderWidth: 2,
        borderJoinStyle: 'round',
        borderCapStyle: 'round',
        tension: 0.32,
        fill: true,
        // Lavis dégradé de la teinte de série — jamais un aplat saturé.
        backgroundColor: (ctx: any) => {
          const zone = ctx.chart?.chartArea;
          if (!zone) return 'rgba(140,47,77,0.10)';
          const g = ctx.chart.ctx.createLinearGradient(0, zone.top, 0, zone.bottom);
          g.addColorStop(0, 'rgba(140,47,77,0.20)');
          g.addColorStop(1, 'rgba(140,47,77,0.01)');
          return g;
        },
        // Étiquetage sélectif : seuls le meilleur jour et le dernier point
        // portent un repère. Un point sur chaque valeur devient du bruit.
        pointRadius: points.map((_, i) =>
          points.length <= 10 || i === iMax || i === dernier ? 4.5 : 0),
        pointBackgroundColor: SERIES[0],
        pointBorderColor: CHROME.surface,
        pointBorderWidth: 2,
        pointHoverRadius: 6,
        pointHoverBackgroundColor: SERIES[0],
        pointHoverBorderColor: CHROME.surface,
        pointHoverBorderWidth: 2
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      layout: { padding: { top: 10, right: 6 } },
      interaction: { mode: 'index', intersect: false },
      plugins: { legend: { display: false }, tooltip: infobulle(dates, montantComplet) },
      scales: { y: axeValeurs(montantCompact, 5), x: axeDates(points.length) }
    }
  };
}

/** Colonnes d'un décompte par jour (réservations reçues, etc.). */
export function colonnesParJour(points: PointJour[], couleur: string, unite: (n: number) => string) {
  const dates = points.map(p => p.date);

  return {
    dates,
    data: {
      labels: dates.map(labelJourCourt),
      datasets: [{
        data: points.map(p => p.valeur),
        backgroundColor: couleur,
        hoverBackgroundColor: couleur,
        maxBarThickness: 22,
        // Extrémité arrondie côté données, carrée sur la ligne de base.
        borderRadius: { topLeft: 4, topRight: 4, bottomLeft: 0, bottomRight: 0 },
        borderSkipped: 'bottom'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      layout: { padding: { top: 10 } },
      interaction: { mode: 'index', intersect: false },
      plugins: { legend: { display: false }, tooltip: infobulle(dates, unite) },
      scales: { y: axeValeurs(String, 4, true), x: axeDates(points.length) }
    }
  };
}
