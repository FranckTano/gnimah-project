/* @ds-bundle: {"format":3,"namespace":"VendeurCiDesignSystem_f69b94","components":[{"name":"ProductCard","sourcePath":"components/commerce/ProductCard.jsx"},{"name":"SellerCard","sourcePath":"components/commerce/SellerCard.jsx"},{"name":"StatCard","sourcePath":"components/commerce/StatCard.jsx"},{"name":"Avatar","sourcePath":"components/core/Avatar.jsx"},{"name":"Badge","sourcePath":"components/core/Badge.jsx"},{"name":"Button","sourcePath":"components/core/Button.jsx"},{"name":"Chip","sourcePath":"components/core/Chip.jsx"},{"name":"IconButton","sourcePath":"components/core/IconButton.jsx"},{"name":"Input","sourcePath":"components/core/Input.jsx"},{"name":"StarRating","sourcePath":"components/core/StarRating.jsx"},{"name":"BottomNav","sourcePath":"components/navigation/BottomNav.jsx"},{"name":"SearchBar","sourcePath":"components/navigation/SearchBar.jsx"}],"sourceHashes":{"components/commerce/ProductCard.jsx":"fb150df73213","components/commerce/SellerCard.jsx":"3511d6f8d92a","components/commerce/StatCard.jsx":"8329b9963fdb","components/core/Avatar.jsx":"cdb49964b74d","components/core/Badge.jsx":"e72950819f5d","components/core/Button.jsx":"50dc1b27c58a","components/core/Chip.jsx":"1c27f8a26f95","components/core/IconButton.jsx":"eda185fec99c","components/core/Input.jsx":"5da0f86ff1d1","components/core/StarRating.jsx":"f6da489a94a6","components/navigation/BottomNav.jsx":"03366c103d17","components/navigation/SearchBar.jsx":"9243fa354f95","ui_kits/mobile-app/app.jsx":"e2bfbb281a82","ui_kits/mobile-app/data.js":"4614c540ff06","ui_kits/mobile-app/screens-buyer.jsx":"7843e7c5571d","ui_kits/mobile-app/screens-seller.jsx":"0c8c39355c5a","ui_kits/mobile-app/ui.jsx":"9f95e9a20e25"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.VendeurCiDesignSystem_f69b94 = window.VendeurCiDesignSystem_f69b94 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/commerce/StatCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — StatCard
 * Metric tile for the seller dashboard & statistics (Vues, Clics WhatsApp,
 * Clics Appel, Conversion). Color-coded icon chip.
 */
function StatCard({
  icon,
  value,
  label,
  tone = 'blue',
  // blue | green | gray | amber
  trend = null,
  // e.g. "+12%"
  style = {},
  ...rest
}) {
  const tones = {
    blue: {
      bg: 'var(--color-primary-soft)',
      fg: 'var(--color-primary)'
    },
    green: {
      bg: 'var(--wa-100)',
      fg: 'var(--color-whatsapp-dark)'
    },
    gray: {
      bg: 'var(--gray-100)',
      fg: 'var(--gray-600)'
    },
    amber: {
      bg: 'var(--amber-100)',
      fg: 'var(--orange-700)'
    }
  };
  const t = tones[tone] || tones.blue;
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      flex: 1,
      minWidth: 0,
      background: 'var(--surface-card)',
      borderRadius: 'var(--radius-lg)',
      padding: '13px 13px 14px',
      boxShadow: 'var(--shadow-sm)',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      width: 30,
      height: 30,
      borderRadius: 'var(--radius-xs)',
      marginBottom: 8,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: t.bg,
      color: t.fg
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${icon}`,
    style: {
      fontSize: 17
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'baseline',
      gap: 5
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--fw-extrabold) 21px/1 var(--font-sans)',
      color: 'var(--text-body)',
      letterSpacing: '-0.01em'
    }
  }, value), trend && /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--fw-bold) 11px/1 var(--font-sans)',
      color: 'var(--color-success)'
    }
  }, trend)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--fw-medium) 11px/1.2 var(--font-sans)',
      color: 'var(--text-muted)',
      marginTop: 2
    }
  }, label));
}
Object.assign(__ds_scope, { StatCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/commerce/StatCard.jsx", error: String((e && e.message) || e) }); }

// components/core/Avatar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — Avatar
 * Photo de boutique / profil. Fallback : initiales sur le tint de marque.
 * Anneau + pastille « vérifié » optionnels.
 */
function Avatar({
  src = null,
  name = '',
  size = 48,
  shape = 'circle',
  // circle | rounded
  verified = false,
  ring = false,
  style = {},
  ...rest
}) {
  const initials = name.split(' ').filter(Boolean).slice(0, 2).map(w => w[0]).join('').toUpperCase() || '?';
  const radius = shape === 'circle' ? '50%' : 'var(--radius-md)';
  const ringStyle = ring ? {
    boxShadow: '0 0 0 2px var(--surface-card), 0 0 0 4px var(--color-primary-soft)'
  } : {};
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      position: 'relative',
      display: 'inline-flex',
      flexShrink: 0,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      width: size,
      height: size,
      borderRadius: radius,
      overflow: 'hidden',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: src ? `center/cover no-repeat url(${src})` : 'var(--color-primary-soft)',
      color: 'var(--color-primary)',
      font: `var(--fw-bold) ${Math.round(size * 0.38)}px/1 var(--font-sans)`,
      ...ringStyle
    }
  }, !src && initials), verified && /*#__PURE__*/React.createElement("i", {
    className: "ti ti-rosette-discount-check-filled",
    style: {
      position: 'absolute',
      right: -2,
      bottom: -2,
      fontSize: Math.max(14, size * 0.34),
      color: 'var(--color-primary)',
      background: 'var(--surface-card)',
      borderRadius: '50%'
    }
  }));
}
Object.assign(__ds_scope, { Avatar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Avatar.jsx", error: String((e && e.message) || e) }); }

// components/core/Badge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — Badge
 * Pastille de statut / libellé. Les presets couvrent les vrais états :
 * statut produit (actif/pause/expiré), plans vendeur, « Vedette », « Négociable ».
 */
function Badge({
  children,
  variant = 'neutral',
  icon = null,
  // nom d'icône Tabler
  size = 'md',
  // sm | md
  style = {},
  ...rest
}) {
  const variants = {
    neutral: {
      background: 'var(--gray-100)',
      color: 'var(--gray-600)'
    },
    primary: {
      background: 'var(--color-primary-soft)',
      color: 'var(--color-primary)'
    },
    active: {
      background: 'var(--status-active-bg)',
      color: 'var(--status-active-fg)'
    },
    paused: {
      background: 'var(--status-paused-bg)',
      color: 'var(--status-paused-fg)'
    },
    expired: {
      background: 'var(--status-expired-bg)',
      color: 'var(--status-expired-fg)'
    },
    vedette: {
      background: 'var(--amber-100)',
      color: 'var(--orange-700)'
    },
    negociable: {
      background: 'var(--color-primary-soft)',
      color: 'var(--color-primary)'
    },
    whatsapp: {
      background: 'var(--wa-100)',
      color: 'var(--color-whatsapp-dark)'
    },
    // solides (sur photo ou surface colorée)
    solidBlue: {
      background: 'var(--color-primary)',
      color: '#fff'
    },
    solidAmber: {
      background: 'var(--amber-500)',
      color: '#fff'
    },
    // plans
    free: {
      background: 'var(--plan-free-bg)',
      color: 'var(--plan-free-fg)'
    },
    essentiel: {
      background: 'var(--plan-essentiel-bg)',
      color: 'var(--plan-essentiel-fg)'
    },
    pro: {
      background: 'var(--plan-pro-bg)',
      color: 'var(--plan-pro-fg)'
    }
  };
  const v = variants[variant] || variants.neutral;
  const dims = size === 'sm' ? {
    padding: '3px 8px',
    fontSize: 10.5,
    iconSize: 12,
    gap: 3
  } : {
    padding: '4px 10px',
    fontSize: 11.5,
    iconSize: 13,
    gap: 4
  };
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: dims.gap,
      padding: dims.padding,
      borderRadius: 'var(--radius-pill)',
      font: `var(--fw-bold) ${dims.fontSize}px/1 var(--font-sans)`,
      letterSpacing: '0.01em',
      whiteSpace: 'nowrap',
      ...v,
      ...style
    }
  }, rest), icon && /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${icon}`,
    style: {
      fontSize: dims.iconSize
    }
  }), children);
}
Object.assign(__ds_scope, { Badge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Badge.jsx", error: String((e && e.message) || e) }); }

// components/commerce/ProductCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — ProductCard
 * The 2-column grid card across Accueil, Catalogue, Favoris, Boutique.
 * Photo (or gradient placeholder), favorite heart, optional "Vedette" badge,
 * title, price in FCFA, seller line.
 */
function ProductCard({
  title,
  price = null,
  // number → formatted; null → "Prix sur demande"
  currency = 'FCFA',
  photo = null,
  // image URL
  ghostIcon = 'photo',
  // Tabler icon shown when no photo
  accent = 'linear-gradient(140deg,#4f86d6,#2a5fb0)',
  seller = null,
  vedette = false,
  negociable = false,
  favorite = false,
  unavailable = false,
  // "Plus disponible" (deleted by seller)
  onToggleFavorite = null,
  onClick = null,
  style = {},
  ...rest
}) {
  const formattedPrice = price != null ? Number(price).toLocaleString('fr-FR').replace(/,/g, ' ') : null;
  return /*#__PURE__*/React.createElement("article", _extends({
    onClick: onClick,
    style: {
      background: 'var(--surface-card)',
      borderRadius: 'var(--radius-lg)',
      overflow: 'hidden',
      boxShadow: 'var(--shadow-sm)',
      cursor: onClick ? 'pointer' : 'default',
      opacity: unavailable ? 0.6 : 1,
      position: 'relative',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 124,
      position: 'relative',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: photo ? `center/cover no-repeat url(${photo})` : accent
    }
  }, !photo && /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${ghostIcon}`,
    style: {
      fontSize: 44,
      color: 'rgba(255,255,255,0.55)'
    }
  }), vedette && /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      top: 9,
      left: 9
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Badge, {
    variant: "vedette",
    size: "sm",
    icon: "rosette-discount-check-filled"
  }, "Vedette")), /*#__PURE__*/React.createElement("button", {
    onClick: e => {
      e.stopPropagation();
      onToggleFavorite && onToggleFavorite();
    },
    "aria-label": "Favori",
    style: {
      position: 'absolute',
      top: 9,
      right: 9,
      width: 30,
      height: 30,
      borderRadius: '50%',
      border: 'none',
      background: 'rgba(255,255,255,0.92)',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      color: favorite ? 'var(--color-error)' : 'var(--text-muted)',
      boxShadow: 'var(--shadow-xs)'
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: `ti ti-heart${favorite ? '-filled' : ''}`,
    style: {
      fontSize: 17
    }
  })), unavailable && /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      inset: 0,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'rgba(20,24,43,0.45)',
      color: '#fff',
      font: 'var(--fw-bold) var(--fs-sm)/1 var(--font-sans)'
    }
  }, "Plus disponible")), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '10px 12px 13px'
    }
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: 'var(--fw-semibold) 13.5px/1.25 var(--font-sans)',
      color: 'var(--text-body)',
      margin: 0,
      minHeight: 34,
      display: '-webkit-box',
      WebkitLineClamp: 2,
      WebkitBoxOrient: 'vertical',
      overflow: 'hidden'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 6,
      marginTop: 6
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--fw-extrabold) 16px/1 var(--font-sans)',
      color: 'var(--color-price)',
      letterSpacing: '-0.01em'
    }
  }, formattedPrice ? /*#__PURE__*/React.createElement(React.Fragment, null, formattedPrice, " ", /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 11,
      fontWeight: 'var(--fw-bold)',
      opacity: 0.7
    }
  }, currency)) : /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 13.5
    }
  }, "Prix sur demande")), negociable && /*#__PURE__*/React.createElement(__ds_scope.Badge, {
    variant: "negociable",
    size: "sm"
  }, "N\xE9go.")), seller && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 4,
      marginTop: 5,
      font: 'var(--fw-medium) 11.5px/1 var(--font-sans)',
      color: 'var(--text-disabled)'
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: "ti ti-map-pin",
    style: {
      fontSize: 13
    }
  }), seller)));
}
Object.assign(__ds_scope, { ProductCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/commerce/ProductCard.jsx", error: String((e && e.message) || e) }); }

// components/core/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — Button
 * CTA bleu principal + variantes dédiées WhatsApp / Appel
 * (le contact est l'action centrale de toute l'app).
 */
function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  icon = null,
  // nom d'icône Tabler, ex. "brand-whatsapp"
  iconRight = null,
  disabled = false,
  loading = false,
  children,
  style = {},
  ...rest
}) {
  const sizes = {
    sm: {
      height: 40,
      padding: '0 16px',
      fontSize: 14,
      radius: 'var(--radius-sm)',
      icon: 18,
      gap: 6
    },
    md: {
      height: 48,
      padding: '0 20px',
      fontSize: 15,
      radius: 'var(--radius-md)',
      icon: 20,
      gap: 8
    },
    lg: {
      height: 52,
      padding: '0 24px',
      fontSize: 15.5,
      radius: 'var(--radius-md)',
      icon: 21,
      gap: 8
    }
  };
  const s = sizes[size] || sizes.md;
  const variants = {
    primary: {
      background: 'var(--color-primary)',
      color: 'var(--color-on-primary)',
      boxShadow: 'var(--shadow-primary)',
      border: 'none'
    },
    secondary: {
      background: 'var(--surface-card)',
      color: 'var(--color-primary)',
      border: '2px solid var(--color-primary)',
      boxShadow: 'none'
    },
    whatsapp: {
      background: 'var(--color-whatsapp)',
      color: '#fff',
      boxShadow: 'var(--shadow-whatsapp)',
      border: 'none'
    },
    call: {
      background: 'var(--surface-card)',
      color: 'var(--color-call)',
      border: '2px solid var(--color-call)',
      boxShadow: 'none'
    },
    ghost: {
      background: 'var(--color-primary-soft)',
      color: 'var(--color-primary)',
      border: 'none',
      boxShadow: 'none'
    },
    danger: {
      background: 'var(--color-error)',
      color: '#fff',
      border: 'none',
      boxShadow: 'none'
    }
  };
  const v = variants[variant] || variants.primary;
  return /*#__PURE__*/React.createElement("button", _extends({
    disabled: disabled || loading,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: s.gap,
      height: s.height,
      padding: s.padding,
      borderRadius: s.radius,
      font: `var(--fw-bold) ${s.fontSize}px/1 var(--font-sans)`,
      letterSpacing: '-0.01em',
      cursor: disabled ? 'not-allowed' : 'pointer',
      width: fullWidth ? '100%' : 'auto',
      opacity: disabled ? 0.5 : 1,
      transition: 'transform var(--dur-fast) var(--ease-out), filter var(--dur-fast) var(--ease-out)',
      WebkitTapHighlightColor: 'transparent',
      whiteSpace: 'nowrap',
      ...v,
      ...style
    },
    onMouseDown: e => {
      if (!disabled) e.currentTarget.style.transform = 'scale(var(--press-scale))';
    },
    onMouseUp: e => {
      e.currentTarget.style.transform = 'scale(1)';
    },
    onMouseLeave: e => {
      e.currentTarget.style.transform = 'scale(1)';
    }
  }, rest), loading ? /*#__PURE__*/React.createElement("i", {
    className: "ti ti-loader-2",
    style: {
      fontSize: s.icon,
      animation: 'vci-spin 0.7s linear infinite'
    }
  }) : icon && /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${icon}`,
    style: {
      fontSize: s.icon
    }
  }), children, iconRight && !loading && /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${iconRight}`,
    style: {
      fontSize: s.icon
    }
  }));
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Button.jsx", error: String((e && e.message) || e) }); }

// components/core/Chip.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — Chip
 * Pastille de catégorie horizontale (Mode, Électronique…). Sélectionnée = bleu
 * plein, au repos = tint bleu doux. Icône de tête optionnelle.
 */
function Chip({
  children,
  selected = false,
  icon = null,
  onClick = null,
  style = {},
  ...rest
}) {
  return /*#__PURE__*/React.createElement("button", _extends({
    onClick: onClick,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      flexShrink: 0,
      padding: '9px 15px',
      borderRadius: 'var(--radius-pill)',
      cursor: 'pointer',
      border: 'none',
      whiteSpace: 'nowrap',
      font: 'var(--fw-bold) 13.5px/1 var(--font-sans)',
      background: selected ? 'var(--color-primary)' : 'var(--color-primary-soft)',
      color: selected ? '#fff' : 'var(--color-primary)',
      transition: 'transform var(--dur-fast) var(--ease-out), background var(--dur-fast)',
      WebkitTapHighlightColor: 'transparent',
      ...style
    },
    onMouseDown: e => {
      e.currentTarget.style.transform = 'scale(0.95)';
    },
    onMouseUp: e => {
      e.currentTarget.style.transform = 'scale(1)';
    },
    onMouseLeave: e => {
      e.currentTarget.style.transform = 'scale(1)';
    }
  }, rest), icon && /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${icon}`,
    style: {
      fontSize: 16
    }
  }), children);
}
Object.assign(__ds_scope, { Chip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Chip.jsx", error: String((e && e.message) || e) }); }

// components/core/IconButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — IconButton
 * Bouton-icône carré arrondi : actions de barre, boutons flottants sur photo,
 * menus de carte. Trois tons, trois tailles.
 */
function IconButton({
  icon,
  tone = 'neutral',
  // neutral | primary | onPhoto | onBlue
  size = 'md',
  // sm | md | lg
  badge = false,
  // pastille de notification rouge
  ariaLabel,
  style = {},
  ...rest
}) {
  const sizes = {
    sm: {
      box: 36,
      icon: 19,
      radius: 'var(--radius-sm)'
    },
    md: {
      box: 40,
      icon: 21,
      radius: 'var(--radius-md)'
    },
    lg: {
      box: 44,
      icon: 23,
      radius: 'var(--radius-md)'
    }
  };
  const s = sizes[size] || sizes.md;
  const tones = {
    neutral: {
      background: 'var(--surface-card)',
      color: 'var(--text-body)',
      boxShadow: 'var(--shadow-xs)',
      border: '1px solid var(--border-subtle)'
    },
    primary: {
      background: 'var(--color-primary-soft)',
      color: 'var(--color-primary)',
      border: 'none',
      boxShadow: 'none'
    },
    onPhoto: {
      background: 'rgba(255,255,255,0.92)',
      color: 'var(--text-body)',
      boxShadow: 'var(--shadow-float)',
      border: 'none',
      borderRadius: '50%'
    },
    onBlue: {
      background: 'rgba(255,255,255,0.16)',
      color: '#fff',
      border: 'none',
      boxShadow: 'none'
    }
  };
  const t = tones[tone] || tones.neutral;
  return /*#__PURE__*/React.createElement("button", _extends({
    "aria-label": ariaLabel,
    style: {
      position: 'relative',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      width: s.box,
      height: s.box,
      borderRadius: s.radius,
      cursor: 'pointer',
      WebkitTapHighlightColor: 'transparent',
      flexShrink: 0,
      transition: 'transform var(--dur-fast) var(--ease-out)',
      ...t,
      ...style
    },
    onMouseDown: e => {
      e.currentTarget.style.transform = 'scale(0.92)';
    },
    onMouseUp: e => {
      e.currentTarget.style.transform = 'scale(1)';
    },
    onMouseLeave: e => {
      e.currentTarget.style.transform = 'scale(1)';
    }
  }, rest), /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${icon}`,
    style: {
      fontSize: s.icon
    }
  }), badge && /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      top: s.box * 0.22,
      right: s.box * 0.22,
      width: 8,
      height: 8,
      borderRadius: '50%',
      background: 'var(--color-error)',
      border: '2px solid var(--surface-card)'
    }
  }));
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/core/Input.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — Input
 * Rounded text field with optional label, leading icon, prefix/suffix
 * (e.g. "+225", "FCFA"), helper + inline error. Used across forms & auth.
 */
function Input({
  label = null,
  icon = null,
  // leading Tabler icon
  prefix = null,
  // e.g. "+225"
  suffix = null,
  // e.g. "FCFA"
  error = null,
  hint = null,
  type = 'text',
  style = {},
  containerStyle = {},
  ...rest
}) {
  const [focused, setFocused] = React.useState(false);
  const borderColor = error ? 'var(--color-error)' : focused ? 'var(--color-primary)' : 'var(--border-subtle)';
  const ring = error ? 'var(--ring-error)' : 'var(--ring)';
  return /*#__PURE__*/React.createElement("label", {
    style: {
      display: 'block',
      ...containerStyle
    }
  }, label && /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'block',
      marginBottom: 8,
      font: 'var(--fw-semibold) var(--fs-sm)/1 var(--font-sans)',
      color: 'var(--text-body)'
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      height: 'var(--control-h-md)',
      padding: '0 16px',
      background: 'var(--surface-card)',
      borderRadius: 'var(--radius-md)',
      border: `1.5px solid ${borderColor}`,
      boxShadow: focused ? ring : 'none',
      transition: 'border-color var(--dur-fast), box-shadow var(--dur-fast)'
    }
  }, icon && /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${icon}`,
    style: {
      fontSize: 20,
      color: 'var(--text-muted)'
    }
  }), prefix && /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--fw-bold) var(--fs-body)/1 var(--font-sans)',
      color: 'var(--text-body)',
      whiteSpace: 'nowrap'
    }
  }, prefix), /*#__PURE__*/React.createElement("input", _extends({
    type: type,
    onFocus: e => {
      setFocused(true);
      rest.onFocus && rest.onFocus(e);
    },
    onBlur: e => {
      setFocused(false);
      rest.onBlur && rest.onBlur(e);
    },
    style: {
      flex: 1,
      minWidth: 0,
      border: 'none',
      outline: 'none',
      background: 'transparent',
      font: 'var(--fw-medium) var(--fs-body)/1.2 var(--font-sans)',
      color: 'var(--text-body)',
      ...style
    }
  }, rest)), suffix && /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--fw-bold) var(--fs-sm)/1 var(--font-sans)',
      color: 'var(--text-muted)',
      whiteSpace: 'nowrap'
    }
  }, suffix)), (error || hint) && /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 4,
      marginTop: 6,
      font: 'var(--fw-medium) var(--fs-xs)/1.3 var(--font-sans)',
      color: error ? 'var(--color-error)' : 'var(--text-muted)'
    }
  }, error && /*#__PURE__*/React.createElement("i", {
    className: "ti ti-alert-circle",
    style: {
      fontSize: 14
    }
  }), error || hint));
}
Object.assign(__ds_scope, { Input });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Input.jsx", error: String((e && e.message) || e) }); }

// components/core/StarRating.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — StarRating
 * Amber star + numeric note. Display by default; set `interactive` for the
 * "Donner mon avis" modal (1–5 selection).
 */
function StarRating({
  value = 0,
  count = null,
  // number of reviews → shows "· 126 avis"
  size = 'md',
  // sm | md | lg
  interactive = false,
  onChange = null,
  showValue = true,
  style = {},
  ...rest
}) {
  const px = {
    sm: 13,
    md: 15,
    lg: 28
  }[size] || 15;
  const fontPx = {
    sm: 11.5,
    md: 13,
    lg: 16
  }[size] || 13;
  const [hover, setHover] = React.useState(0);
  if (interactive) {
    return /*#__PURE__*/React.createElement("div", _extends({
      style: {
        display: 'inline-flex',
        gap: 6,
        ...style
      }
    }, rest), [1, 2, 3, 4, 5].map(i => /*#__PURE__*/React.createElement("i", {
      key: i,
      className: `ti ti-star${(hover || value) >= i ? '-filled' : ''}`,
      onMouseEnter: () => setHover(i),
      onMouseLeave: () => setHover(0),
      onClick: () => onChange && onChange(i),
      style: {
        fontSize: px,
        cursor: 'pointer',
        color: 'var(--color-star)',
        transition: 'transform var(--dur-fast) var(--ease-spring)',
        transform: (hover || value) >= i ? 'scale(1.08)' : 'scale(1)'
      }
    })));
  }
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 4,
      font: `var(--fw-bold) ${fontPx}px/1 var(--font-sans)`,
      color: 'var(--text-muted)',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("i", {
    className: "ti ti-star-filled",
    style: {
      fontSize: px,
      color: 'var(--color-star)'
    }
  }), showValue && /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--text-body)'
    }
  }, Number(value).toFixed(1)), count != null && /*#__PURE__*/React.createElement("span", {
    style: {
      fontWeight: 'var(--fw-medium)'
    }
  }, "\xB7 ", count, " avis"));
}
Object.assign(__ds_scope, { StarRating });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/StarRating.jsx", error: String((e && e.message) || e) }); }

// components/commerce/SellerCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — SellerCard
 * Compact shop card for the "Vendeurs vedettes" horizontal carousel on Accueil.
 */
function SellerCard({
  name,
  photo = null,
  rating = null,
  vedette = false,
  onClick = null,
  style = {},
  ...rest
}) {
  return /*#__PURE__*/React.createElement("button", _extends({
    onClick: onClick,
    style: {
      flexShrink: 0,
      width: 100,
      background: 'var(--surface-card)',
      borderRadius: 'var(--radius-lg)',
      padding: '12px 8px',
      textAlign: 'center',
      boxShadow: 'var(--shadow-sm)',
      border: 'none',
      cursor: 'pointer',
      position: 'relative',
      WebkitTapHighlightColor: 'transparent',
      ...style
    }
  }, rest), vedette && /*#__PURE__*/React.createElement("i", {
    className: "ti ti-rosette-discount-check-filled",
    style: {
      position: 'absolute',
      top: 8,
      right: 10,
      fontSize: 16,
      color: 'var(--color-primary)'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'center',
      marginBottom: 8
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Avatar, {
    src: photo,
    name: name,
    size: 52,
    ring: true
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--fw-bold) 12.5px/1.2 var(--font-sans)',
      color: 'var(--text-body)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, name), rating != null && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 3,
      display: 'flex',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.StarRating, {
    value: rating,
    size: "sm"
  })));
}
Object.assign(__ds_scope, { SellerCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/commerce/SellerCard.jsx", error: String((e && e.message) || e) }); }

// components/navigation/BottomNav.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — BottomNav
 * Fixed bottom tab bar. Presets for the buyer and seller flows; optional
 * floating "+" FAB for the seller dashboard.
 */
const PRESETS = {
  buyer: [{
    icon: 'home-2',
    label: 'Accueil'
  }, {
    icon: 'layout-grid',
    label: 'Catégories'
  }, {
    icon: 'heart',
    label: 'Favoris'
  }, {
    icon: 'user',
    label: 'Profil'
  }],
  seller: [{
    icon: 'layout-dashboard',
    label: 'Tableau'
  }, {
    icon: 'box',
    label: 'Produits'
  }, {
    icon: 'chart-bar',
    label: 'Stats'
  }, {
    icon: 'user',
    label: 'Profil'
  }]
};
function BottomNav({
  variant = 'buyer',
  // buyer | seller
  items = null,
  // override preset
  active = 0,
  onSelect = null,
  fab = false,
  // floating + button (seller)
  onFab = null,
  style = {},
  ...rest
}) {
  const tabs = items || PRESETS[variant] || PRESETS.buyer;
  return /*#__PURE__*/React.createElement("nav", _extends({
    style: {
      position: 'relative',
      height: 'var(--bottomnav-h)',
      flexShrink: 0,
      background: 'var(--surface-card)',
      borderTop: '1px solid var(--gray-100)',
      boxShadow: 'var(--shadow-nav)',
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-around',
      paddingTop: 11,
      ...style
    }
  }, rest), tabs.map((tab, i) => {
    const on = i === active;
    return /*#__PURE__*/React.createElement("button", {
      key: i,
      onClick: () => onSelect && onSelect(i),
      style: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 3,
        width: 60,
        border: 'none',
        background: 'none',
        cursor: 'pointer',
        color: on ? 'var(--color-primary)' : 'var(--text-disabled)',
        font: 'var(--fw-bold) 10.5px/1 var(--font-sans)',
        WebkitTapHighlightColor: 'transparent'
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: `ti ti-${tab.icon}${on ? '-filled' : ''}`,
      style: {
        fontSize: 24
      }
    }), tab.label);
  }), fab && /*#__PURE__*/React.createElement("button", {
    onClick: onFab,
    "aria-label": "Ajouter un produit",
    style: {
      position: 'absolute',
      right: 18,
      bottom: 'calc(100% + 16px)',
      width: 58,
      height: 58,
      borderRadius: 19,
      border: 'none',
      background: 'var(--color-primary)',
      color: '#fff',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      boxShadow: 'var(--shadow-fab)'
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: "ti ti-plus",
    style: {
      fontSize: 28
    }
  })));
}
Object.assign(__ds_scope, { BottomNav });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/BottomNav.jsx", error: String((e && e.message) || e) }); }

// components/navigation/SearchBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Vendeur.ci — SearchBar
 * The rounded search field on Accueil / Catalogue. `button` mode is a tappable
 * placeholder that routes to search; otherwise it's a live input with optional
 * filter button.
 */
function SearchBar({
  value = '',
  placeholder = 'Rechercher un produit...',
  asButton = false,
  onChange = null,
  onFilter = null,
  filterActive = false,
  style = {},
  ...rest
}) {
  const field = /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      height: 50,
      padding: '0 16px',
      background: 'var(--surface-card)',
      borderRadius: 'var(--radius-md)',
      border: '1px solid var(--border-subtle)'
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: "ti ti-search",
    style: {
      fontSize: 20,
      color: 'var(--text-disabled)'
    }
  }), asButton ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--fw-medium) var(--fs-body)/1 var(--font-sans)',
      color: 'var(--text-disabled)'
    }
  }, placeholder) : /*#__PURE__*/React.createElement("input", {
    value: value,
    placeholder: placeholder,
    onChange: e => onChange && onChange(e.target.value),
    style: {
      flex: 1,
      minWidth: 0,
      border: 'none',
      outline: 'none',
      background: 'transparent',
      font: 'var(--fw-medium) var(--fs-body)/1 var(--font-sans)',
      color: 'var(--text-body)'
    }
  }));
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: asButton ? rest.onClick : undefined,
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      cursor: asButton ? 'pointer' : 'default',
      ...style
    }
  }, asButton ? {} : rest), field, onFilter && /*#__PURE__*/React.createElement("button", {
    onClick: e => {
      e.stopPropagation();
      onFilter();
    },
    "aria-label": "Filtres",
    style: {
      position: 'relative',
      width: 50,
      height: 50,
      borderRadius: 'var(--radius-md)',
      border: 'none',
      background: filterActive ? 'var(--color-primary)' : 'var(--color-primary-soft)',
      color: filterActive ? '#fff' : 'var(--color-primary)',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: "ti ti-adjustments-horizontal",
    style: {
      fontSize: 22
    }
  }), filterActive && /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      top: 9,
      right: 9,
      width: 8,
      height: 8,
      borderRadius: '50%',
      background: 'var(--color-error)',
      border: '2px solid var(--color-primary)'
    }
  })));
}
Object.assign(__ds_scope, { SearchBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/SearchBar.jsx", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/app.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Vendeur.ci UI kit — app router + phone shell. Mounts to #root.
(function () {
  const {
    useState,
    useEffect,
    useCallback
  } = React;
  const DS = () => window.VendeurCiDesignSystem_f69b94;
  const {
    Toast,
    BottomSheet
  } = window.VKUI;
  const B = window.VKBuyer,
    S = window.VKSeller,
    D = window.VKData;
  const BUYER_TABS = ['accueil', 'catalogue', 'favoris', 'profil'];
  function App() {
    const [stack, setStack] = useState([{
      route: 'accueil',
      params: null
    }]);
    const [favs, setFavs] = useState(() => new Set(['p3']));
    const [toast, setToast] = useState(null);
    const [sheet, setSheet] = useState(null);
    const [scale, setScale] = useState(1);
    const top = stack[stack.length - 1];
    useEffect(() => {
      const fit = () => {
        const s = Math.min(1, (window.innerHeight - 48) / 812, (window.innerWidth - 32) / 375);
        setScale(s);
      };
      fit();
      window.addEventListener('resize', fit);
      return () => window.removeEventListener('resize', fit);
    }, []);
    const nav = {
      go: (route, params = null) => setStack(st => [...st, {
        route,
        params
      }]),
      back: () => setStack(st => st.length > 1 ? st.slice(0, -1) : st),
      tab: i => setStack([{
        route: BUYER_TABS[i],
        params: null
      }]),
      stab: i => {
        if (i === 0) setStack([{
          route: 'dashboard',
          params: null
        }]);else if (i === 1) setStack([{
          route: 'produits',
          params: null
        }]);else setToast({
          message: 'Bientôt disponible',
          icon: 'sparkles',
          tone: 'info'
        });
      },
      startSeller: () => setStack([{
        route: 'dashboard',
        params: null
      }]),
      isFav: id => favs.has(id),
      toggleFav: id => setFavs(s => {
        const n = new Set(s);
        n.has(id) ? n.delete(id) : n.add(id);
        return n;
      }),
      toast: (message, opts = {}) => setToast({
        message,
        icon: opts.icon || 'check',
        tone: opts.tone || 'success'
      }),
      sheet: kind => setSheet(kind),
      closeSheet: () => setSheet(null)
    };
    const SCREENS = {
      accueil: B.Accueil,
      catalogue: B.Catalogue,
      favoris: B.Favoris,
      profil: B.Profil,
      produit: B.Produit,
      'auth-phone': S.AuthPhone,
      'auth-otp': S.AuthOtp,
      onboarding: S.Onboarding,
      dashboard: S.Dashboard,
      produits: S.Produits,
      'add-product': S.AddProduct
    };
    const Screen = SCREENS[top.route] || B.Accueil;
    return /*#__PURE__*/React.createElement("div", {
      style: {
        position: 'fixed',
        inset: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        background: 'radial-gradient(circle at 30% 0%, #eef2f8, #dde2ec)'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        width: 375,
        height: 812,
        transform: `scale(${scale})`,
        transformOrigin: 'center',
        position: 'relative'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        position: 'absolute',
        inset: 0,
        borderRadius: 46,
        background: '#11131a',
        boxShadow: '0 2px 6px rgba(20,30,60,.1), 0 40px 80px -20px rgba(20,30,60,.4)'
      }
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        position: 'absolute',
        inset: 7,
        borderRadius: 40,
        overflow: 'hidden',
        background: 'var(--surface-page)',
        display: 'flex',
        flexDirection: 'column'
      }
    }, /*#__PURE__*/React.createElement(Screen, {
      key: top.route + (top.params || ''),
      nav: nav,
      params: top.params
    }), sheet && /*#__PURE__*/React.createElement(SheetContent, {
      kind: sheet,
      nav: nav
    }), toast && /*#__PURE__*/React.createElement(Toast, _extends({}, toast, {
      onDone: () => setToast(null)
    })))));
  }
  function SheetContent({
    kind,
    nav
  }) {
    const {
      Button,
      Chip
    } = DS();
    if (kind === 'filters') {
      return /*#__PURE__*/React.createElement(BottomSheet, {
        title: "Filtres",
        onClose: nav.closeSheet
      }, /*#__PURE__*/React.createElement("div", {
        style: {
          font: 'var(--fw-semibold) 13px/1 var(--font-sans)',
          color: 'var(--text-body)',
          marginBottom: 10
        }
      }, "Trier par"), /*#__PURE__*/React.createElement("div", {
        style: {
          display: 'flex',
          gap: 9,
          flexWrap: 'wrap',
          marginBottom: 18
        }
      }, /*#__PURE__*/React.createElement(Chip, {
        selected: true
      }, "Plus r\xE9cents"), /*#__PURE__*/React.createElement(Chip, null, "Plus vus"), /*#__PURE__*/React.createElement(Chip, null, "Prix croissant"), /*#__PURE__*/React.createElement(Chip, null, "Prix d\xE9croissant")), /*#__PURE__*/React.createElement(Button, {
        variant: "primary",
        size: "lg",
        fullWidth: true,
        onClick: nav.closeSheet
      }, "Appliquer les filtres"));
    }
    if (kind === 'product-menu') {
      const items = [{
        icon: 'pencil',
        label: 'Modifier',
        tone: 'body'
      }, {
        icon: 'rocket',
        label: 'Booster ce produit',
        tone: 'primary'
      }, {
        icon: 'player-pause',
        label: 'Mettre en pause',
        tone: 'body'
      }, {
        icon: 'trash',
        label: 'Supprimer',
        tone: 'danger'
      }];
      return /*#__PURE__*/React.createElement(BottomSheet, {
        onClose: nav.closeSheet
      }, items.map((it, i) => /*#__PURE__*/React.createElement("button", {
        key: i,
        onClick: () => {
          nav.closeSheet();
          nav.toast(it.label, {
            icon: it.icon
          });
        },
        style: {
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          gap: 13,
          padding: '14px 4px',
          border: 'none',
          borderBottom: i < 3 ? '1px solid var(--gray-100)' : 'none',
          background: 'none',
          cursor: 'pointer',
          font: 'var(--fw-semibold) 15px/1 var(--font-sans)',
          textAlign: 'left',
          color: it.tone === 'danger' ? 'var(--color-error)' : it.tone === 'primary' ? 'var(--color-primary)' : 'var(--text-body)'
        }
      }, /*#__PURE__*/React.createElement("i", {
        className: `ti ti-${it.icon}`,
        style: {
          fontSize: 22
        }
      }), it.label)));
    }
    return null;
  }
  ReactDOM.createRoot(document.getElementById('root')).render(/*#__PURE__*/React.createElement(App, null));
})();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/app.jsx", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/data.js
try { (() => {
// Vendeur.ci UI kit — demo data (fake content, FR / Côte d'Ivoire)
window.VKData = function () {
  const G = {
    blue: 'linear-gradient(140deg,#4f86d6,#2a5fb0)',
    pink: 'linear-gradient(140deg,#ec6b9d,#c0457a)',
    orange: 'linear-gradient(140deg,#f0a458,#d97b32)',
    green: 'linear-gradient(140deg,#56b88a,#2f8f64)',
    violet: 'linear-gradient(140deg,#8a7fe0,#5f50c4)',
    teal: 'linear-gradient(140deg,#5fb6c4,#2f8a9c)',
    slate: 'linear-gradient(140deg,#7c8ba1,#4a5468)',
    rose: 'linear-gradient(140deg,#e8836f,#cf5a45)'
  };
  const categories = [{
    id: 'mode',
    label: 'Mode',
    icon: 'shirt'
  }, {
    id: 'elec',
    label: 'Électronique',
    icon: 'device-mobile'
  }, {
    id: 'alim',
    label: 'Alimentation',
    icon: 'leaf'
  }, {
    id: 'maison',
    label: 'Maison',
    icon: 'sofa'
  }, {
    id: 'beaute',
    label: 'Beauté',
    icon: 'sparkles'
  }, {
    id: 'auto',
    label: 'Automobile',
    icon: 'car'
  }, {
    id: 'sport',
    label: 'Sport',
    icon: 'ball-football'
  }, {
    id: 'bebe',
    label: 'Bébé',
    icon: 'baby-carriage'
  }];
  const sellers = [{
    id: 's1',
    name: 'Awa Couture',
    rating: 4.9,
    reviews: 213,
    vedette: true,
    cat: 'Mode',
    grad: G.pink
  }, {
    id: 's2',
    name: 'Tech Abidjan',
    rating: 4.7,
    reviews: 168,
    vedette: true,
    cat: 'Électronique',
    grad: G.blue
  }, {
    id: 's3',
    name: 'Bio Market CI',
    rating: 4.8,
    reviews: 96,
    vedette: false,
    cat: 'Alimentation',
    grad: G.green
  }, {
    id: 's4',
    name: 'Sneakers CI',
    rating: 4.8,
    reviews: 126,
    vedette: true,
    cat: 'Mode',
    grad: G.rose
  }, {
    id: 's5',
    name: 'Kouassi Déco',
    rating: 4.5,
    reviews: 54,
    vedette: false,
    cat: 'Maison',
    grad: G.violet
  }];
  const products = [{
    id: 'p1',
    title: 'iPhone 13 Pro 128 Go',
    price: 285000,
    cat: 'Électronique',
    sellerId: 's2',
    grad: G.blue,
    ghost: 'device-mobile',
    vedette: true,
    video: true,
    nego: false,
    photos: 5,
    desc: "iPhone 13 Pro 128 Go, état neuf, débloqué tous opérateurs. Batterie 98%. Livré avec boîte, câble et facture. Possibilité de voir avant achat sur Abidjan (Cocody)."
  }, {
    id: 'p2',
    title: 'Robe wax sur mesure cousue main',
    price: 15000,
    cat: 'Mode',
    sellerId: 's1',
    grad: G.pink,
    ghost: 'shirt',
    vedette: false,
    video: false,
    nego: true,
    photos: 4,
    desc: "Robe en wax authentique, cousue main à vos mesures. Choix du motif disponible. Délai 3 à 5 jours. Livraison Abidjan."
  }, {
    id: 'p3',
    title: 'Sneakers Nike Air Max neuves',
    price: 35000,
    cat: 'Mode',
    sellerId: 's4',
    grad: G.rose,
    ghost: 'shoe',
    vedette: true,
    video: true,
    nego: true,
    photos: 6,
    desc: "Paire neuve jamais portée, pointure 42. Modèle authentique avec facture. Livraison possible sur Abidjan."
  }, {
    id: 'p4',
    title: 'Régime de bananes plantain bio',
    price: null,
    cat: 'Alimentation',
    sellerId: 's3',
    grad: G.green,
    ghost: 'leaf',
    vedette: false,
    video: false,
    nego: false,
    photos: 3,
    desc: "Bananes plantain bio cultivées à Agboville. Vente au régime ou au kilo. Prix selon quantité, contactez-nous."
  }, {
    id: 'p5',
    title: 'Casque Bluetooth sans fil',
    price: 12500,
    cat: 'Électronique',
    sellerId: 's2',
    grad: G.teal,
    ghost: 'headphones',
    vedette: false,
    video: false,
    nego: true,
    photos: 4,
    desc: "Casque sans fil, autonomie 30h, réduction de bruit. Neuf sous emballage. Garantie 6 mois."
  }, {
    id: 'p6',
    title: 'Sac à main cuir véritable',
    price: 22000,
    cat: 'Mode',
    sellerId: 's1',
    grad: G.violet,
    ghost: 'bag',
    vedette: false,
    video: false,
    nego: true,
    photos: 5,
    desc: "Sac en cuir véritable fait main, plusieurs coloris. Pièce unique. Idéal cadeau."
  }, {
    id: 'p7',
    title: 'Montre connectée sport',
    price: 18000,
    cat: 'Électronique',
    sellerId: 's2',
    grad: G.slate,
    ghost: 'device-watch',
    vedette: false,
    video: false,
    nego: false,
    photos: 4,
    desc: "Montre connectée, suivi cardio, notifications, étanche. Neuve. Plusieurs bracelets fournis."
  }, {
    id: 'p8',
    title: 'Table basse bois massif',
    price: 45000,
    cat: 'Maison',
    sellerId: 's5',
    grad: G.orange,
    ghost: 'sofa',
    vedette: false,
    video: true,
    nego: true,
    photos: 5,
    desc: "Table basse artisanale en bois massif (iroko). Finition vernie. Livraison et montage sur Abidjan."
  }];
  const sellerProducts = [{
    id: 'sp1',
    title: 'Robe wax sur mesure',
    price: 15000,
    grad: G.pink,
    ghost: 'shirt',
    status: 'active',
    views: 842,
    wa: 64
  }, {
    id: 'sp2',
    title: 'Sac à main cuir véritable',
    price: 22000,
    grad: G.violet,
    ghost: 'bag',
    status: 'active',
    views: 391,
    wa: 28
  }, {
    id: 'sp3',
    title: 'Ensemble pagne 2 pièces',
    price: 18000,
    grad: G.rose,
    ghost: 'shirt',
    status: 'active',
    views: 254,
    wa: 19
  }, {
    id: 'sp4',
    title: 'Foulard en soie imprimé',
    price: 6500,
    grad: G.teal,
    ghost: 'shirt',
    status: 'paused',
    views: 120,
    wa: 4
  }];
  const byId = id => products.find(p => p.id === id);
  const sellerById = id => sellers.find(s => s.id === id);
  return {
    G,
    categories,
    sellers,
    products,
    sellerProducts,
    byId,
    sellerById
  };
}();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/data.js", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/screens-buyer.jsx
try { (() => {
// Vendeur.ci UI kit — buyer flow screens. Exposed on window.VKBuyer.
(function () {
  const DS = () => window.VendeurCiDesignSystem_f69b94;
  const {
    StatusBar,
    AppBar,
    SectionHead,
    EmptyState
  } = window.VKUI;
  const D = window.VKData;
  const fmt = n => Number(n).toLocaleString('fr-FR').replace(/,/g, ' ');
  function Scroll({
    children,
    pad = true
  }) {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        overflowY: 'auto',
        overflowX: 'hidden',
        padding: pad ? '0 16px 16px' : 0,
        display: 'flex',
        flexDirection: 'column',
        gap: 18
      }
    }, children);
  }

  /* ---------------- ACCUEIL ---------------- */
  function Accueil({
    nav
  }) {
    const {
      IconButton,
      SearchBar,
      Chip,
      SellerCard,
      ProductCard,
      BottomNav
    } = DS();
    const [cat, setCat] = React.useState('all');
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement(AppBar, {
      logo: true,
      right: /*#__PURE__*/React.createElement("div", {
        style: {
          display: 'flex',
          gap: 8
        }
      }, /*#__PURE__*/React.createElement(IconButton, {
        icon: "search",
        onClick: () => nav.tab(1)
      }), /*#__PURE__*/React.createElement(IconButton, {
        icon: "bell",
        badge: true
      }))
    }), /*#__PURE__*/React.createElement(Scroll, null, /*#__PURE__*/React.createElement(SearchBar, {
      asButton: true,
      onClick: () => nav.tab(1)
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 9,
        overflowX: 'auto',
        margin: '0 -16px',
        padding: '0 16px'
      }
    }, /*#__PURE__*/React.createElement(Chip, {
      selected: cat === 'all',
      onClick: () => setCat('all')
    }, "Tout"), D.categories.map(c => /*#__PURE__*/React.createElement(Chip, {
      key: c.id,
      icon: c.icon,
      selected: cat === c.id,
      onClick: () => setCat(c.id)
    }, c.label))), /*#__PURE__*/React.createElement("div", {
      style: {
        position: 'relative',
        overflow: 'hidden',
        borderRadius: 'var(--radius-lg)',
        padding: '18px 20px',
        color: '#fff',
        background: 'linear-gradient(120deg, var(--blue-600), var(--blue-700))',
        boxShadow: 'var(--shadow-primary)'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        position: 'absolute',
        right: -30,
        top: -30,
        width: 150,
        height: 150,
        borderRadius: '50%',
        background: 'rgba(255,255,255,.12)'
      }
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-bold) 11px/1 var(--font-sans)',
        letterSpacing: '0.08em',
        textTransform: 'uppercase',
        opacity: 0.85
      }
    }, "Promo de la semaine"), /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-extrabold) 20px/1.15 var(--font-sans)',
        marginTop: 6,
        maxWidth: '72%',
        letterSpacing: '-0.01em'
      }
    }, "Vendez plus, soyez vu partout"), /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 12,
        display: 'inline-flex',
        font: 'var(--fw-bold) 12.5px/1 var(--font-sans)',
        background: '#fff',
        color: 'var(--color-primary)',
        padding: '7px 13px',
        borderRadius: 100
      }
    }, "D\xE9couvrir \u2192")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SectionHead, {
      title: "Vendeurs vedettes",
      onAction: () => {}
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 12,
        overflowX: 'auto',
        margin: '12px -16px 0',
        padding: '0 16px'
      }
    }, D.sellers.filter(s => s.vedette).map(s => /*#__PURE__*/React.createElement(SellerCard, {
      key: s.id,
      name: s.name,
      rating: s.rating,
      vedette: s.vedette
    })))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SectionHead, {
      title: "Produits r\xE9cents",
      onAction: () => nav.tab(1)
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: 13,
        marginTop: 12
      }
    }, D.products.slice(0, 6).map(p => /*#__PURE__*/React.createElement(ProductCard, {
      key: p.id,
      title: p.title,
      price: p.price,
      accent: p.grad,
      ghostIcon: p.ghost,
      seller: D.sellerById(p.sellerId).name,
      vedette: p.vedette,
      negociable: p.nego,
      favorite: nav.isFav(p.id),
      onToggleFavorite: () => nav.toggleFav(p.id),
      onClick: () => nav.go('produit', p.id)
    }))))), /*#__PURE__*/React.createElement(BottomNav, {
      variant: "buyer",
      active: 0,
      onSelect: nav.tab
    }));
  }

  /* ---------------- CATALOGUE ---------------- */
  function Catalogue({
    nav
  }) {
    const {
      SearchBar,
      ProductCard,
      BottomNav,
      Chip
    } = DS();
    const [q, setQ] = React.useState('');
    const [cat, setCat] = React.useState('all');
    const list = D.products.filter(p => (cat === 'all' || p.cat === D.categories.find(c => c.id === cat)?.label) && (!q || p.title.toLowerCase().includes(q.toLowerCase())));
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement("div", {
      style: {
        padding: '4px 16px 12px',
        flexShrink: 0
      }
    }, /*#__PURE__*/React.createElement(SearchBar, {
      value: q,
      onChange: setQ,
      onFilter: () => nav.sheet('filters'),
      filterActive: cat !== 'all'
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 9,
        overflowX: 'auto',
        margin: '12px -16px 0',
        padding: '0 16px'
      }
    }, /*#__PURE__*/React.createElement(Chip, {
      selected: cat === 'all',
      onClick: () => setCat('all')
    }, "Tout"), D.categories.map(c => /*#__PURE__*/React.createElement(Chip, {
      key: c.id,
      icon: c.icon,
      selected: cat === c.id,
      onClick: () => setCat(c.id)
    }, c.label)))), /*#__PURE__*/React.createElement(Scroll, null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-semibold) 13px/1 var(--font-sans)',
        color: 'var(--text-muted)'
      }
    }, list.length, " produit", list.length > 1 ? 's' : '', " trouv\xE9", list.length > 1 ? 's' : ''), list.length === 0 ? /*#__PURE__*/React.createElement(EmptyState, {
      icon: "search-off",
      title: "Aucun r\xE9sultat",
      sub: "Essayez une autre cat\xE9gorie ou un autre mot-cl\xE9."
    }) : /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: 13
      }
    }, list.map(p => /*#__PURE__*/React.createElement(ProductCard, {
      key: p.id,
      title: p.title,
      price: p.price,
      accent: p.grad,
      ghostIcon: p.ghost,
      seller: D.sellerById(p.sellerId).name,
      vedette: p.vedette,
      negociable: p.nego,
      favorite: nav.isFav(p.id),
      onToggleFavorite: () => nav.toggleFav(p.id),
      onClick: () => nav.go('produit', p.id)
    })))), /*#__PURE__*/React.createElement(BottomNav, {
      variant: "buyer",
      active: 1,
      onSelect: nav.tab
    }));
  }

  /* ---------------- FAVORIS ---------------- */
  function Favoris({
    nav
  }) {
    const {
      ProductCard,
      BottomNav
    } = DS();
    const favs = D.products.filter(p => nav.isFav(p.id));
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement(AppBar, {
      title: "Favoris"
    }), favs.length === 0 ? /*#__PURE__*/React.createElement(EmptyState, {
      icon: "heart",
      title: "Vous n'avez pas encore de favoris",
      sub: "Touchez le c\u0153ur sur un produit pour le retrouver ici.",
      cta: "D\xE9couvrir des produits",
      onCta: () => nav.tab(0)
    }) : /*#__PURE__*/React.createElement(Scroll, null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-medium) 12.5px/1.4 var(--font-sans)',
        color: 'var(--text-muted)',
        background: 'var(--color-primary-soft)',
        padding: '10px 13px',
        borderRadius: 'var(--radius-md)'
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: "ti ti-info-circle",
      style: {
        marginRight: 6,
        verticalAlign: '-2px'
      }
    }), "Connectez-vous pour sauvegarder vos favoris sur tous vos appareils."), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: 13
      }
    }, favs.map(p => /*#__PURE__*/React.createElement(ProductCard, {
      key: p.id,
      title: p.title,
      price: p.price,
      accent: p.grad,
      ghostIcon: p.ghost,
      seller: D.sellerById(p.sellerId).name,
      vedette: p.vedette,
      negociable: p.nego,
      favorite: true,
      onToggleFavorite: () => nav.toggleFav(p.id),
      onClick: () => nav.go('produit', p.id)
    })))), /*#__PURE__*/React.createElement(BottomNav, {
      variant: "buyer",
      active: 2,
      onSelect: nav.tab
    }));
  }

  /* ---------------- PROFIL (non connecté) ---------------- */
  function Profil({
    nav
  }) {
    const {
      Button,
      BottomNav
    } = DS();
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement(AppBar, {
      title: "Profil"
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 28,
        textAlign: 'center',
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        width: 96,
        height: 96,
        borderRadius: '50%',
        background: 'var(--color-primary-soft)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 8
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: "ti ti-user-circle",
      style: {
        fontSize: 52,
        color: 'var(--color-primary)'
      }
    })), /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-bold) 18px/1.3 var(--font-sans)',
        color: 'var(--text-body)'
      }
    }, "Bienvenue sur Vendeur.ci"), /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-regular) 14px/1.5 var(--font-sans)',
        color: 'var(--text-muted)',
        maxWidth: 250
      }
    }, "Connectez-vous pour profiter de toutes les fonctionnalit\xE9s."), /*#__PURE__*/React.createElement("div", {
      style: {
        width: '100%',
        marginTop: 18,
        display: 'flex',
        flexDirection: 'column',
        gap: 11
      }
    }, /*#__PURE__*/React.createElement(Button, {
      variant: "primary",
      size: "lg",
      fullWidth: true,
      icon: "login-2",
      onClick: () => nav.go('auth-phone', {
        intent: 'buyer'
      })
    }, "Se connecter"), /*#__PURE__*/React.createElement(Button, {
      variant: "secondary",
      size: "lg",
      fullWidth: true,
      icon: "building-store",
      onClick: () => nav.go('auth-phone', {
        intent: 'seller'
      })
    }, "Devenir vendeur"))), /*#__PURE__*/React.createElement(BottomNav, {
      variant: "buyer",
      active: 3,
      onSelect: nav.tab
    }));
  }

  /* ---------------- FICHE PRODUIT ---------------- */
  function Produit({
    nav,
    params
  }) {
    const {
      IconButton,
      Button,
      Badge,
      Avatar,
      StarRating
    } = DS();
    const p = D.byId(params);
    const seller = D.sellerById(p.sellerId);
    const [dot, setDot] = React.useState(0);
    return /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        height: 332,
        flexShrink: 0,
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: p.grad
      }
    }, /*#__PURE__*/React.createElement(StatusBar, {
      tone: "light"
    }), /*#__PURE__*/React.createElement("i", {
      className: `ti ti-${p.ghost}`,
      style: {
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%,-50%)',
        fontSize: 110,
        color: 'rgba(255,255,255,.5)'
      }
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        position: 'absolute',
        top: 50,
        left: 16,
        right: 16,
        display: 'flex',
        justifyContent: 'space-between'
      }
    }, /*#__PURE__*/React.createElement(IconButton, {
      icon: "arrow-left",
      tone: "onPhoto",
      size: "lg",
      onClick: nav.back
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 9
      }
    }, /*#__PURE__*/React.createElement(IconButton, {
      icon: nav.isFav(p.id) ? 'heart-filled' : 'heart',
      tone: "onPhoto",
      size: "lg",
      style: {
        color: nav.isFav(p.id) ? 'var(--color-error)' : undefined
      },
      onClick: () => nav.toggleFav(p.id)
    }), /*#__PURE__*/React.createElement(IconButton, {
      icon: "share-2",
      tone: "onPhoto",
      size: "lg",
      onClick: () => nav.toast('Lien copié', {
        icon: 'link'
      })
    }))), p.video && /*#__PURE__*/React.createElement("div", {
      style: {
        position: 'absolute',
        bottom: 54,
        left: 16,
        display: 'flex',
        alignItems: 'center',
        gap: 5,
        background: 'rgba(0,0,0,.55)',
        color: '#fff',
        padding: '5px 11px',
        borderRadius: 100,
        font: 'var(--fw-bold) 11.5px/1 var(--font-sans)',
        backdropFilter: 'blur(4px)'
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: "ti ti-player-play-filled",
      style: {
        fontSize: 13
      }
    }), "Vid\xE9o"), /*#__PURE__*/React.createElement("div", {
      style: {
        position: 'absolute',
        bottom: 22,
        left: 0,
        right: 0,
        display: 'flex',
        justifyContent: 'center',
        gap: 6
      }
    }, Array.from({
      length: p.photos
    }).map((_, i) => /*#__PURE__*/React.createElement("span", {
      key: i,
      style: {
        width: i === dot ? 20 : 6,
        height: 6,
        borderRadius: 6,
        background: i === dot ? '#fff' : 'rgba(255,255,255,.5)',
        transition: 'width .2s'
      }
    })))), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        overflowY: 'auto',
        background: 'var(--surface-card)',
        borderRadius: '26px 26px 0 0',
        marginTop: -22,
        position: 'relative',
        padding: '22px 18px 24px',
        display: 'flex',
        flexDirection: 'column',
        gap: 16
      }
    }, /*#__PURE__*/React.createElement("h1", {
      style: {
        font: 'var(--fw-extrabold) 21px/1.2 var(--font-sans)',
        letterSpacing: '-0.02em',
        margin: 0,
        color: 'var(--text-body)'
      }
    }, p.title), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        alignItems: 'center',
        gap: 10
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: 'var(--fw-extrabold) 27px/1 var(--font-sans)',
        color: 'var(--color-price)',
        letterSpacing: '-0.02em'
      }
    }, p.price != null ? /*#__PURE__*/React.createElement(React.Fragment, null, fmt(p.price), " ", /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 14,
        fontWeight: 'var(--fw-bold)',
        opacity: 0.65
      }
    }, "FCFA")) : /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 18,
        color: 'var(--text-muted)'
      }
    }, "Prix sur demande")), p.nego && /*#__PURE__*/React.createElement(Badge, {
      variant: "negociable"
    }, "N\xE9gociable")), /*#__PURE__*/React.createElement("button", {
      onClick: () => nav.toast('Boutique du vendeur', {
        icon: 'building-store'
      }),
      style: {
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: 13,
        borderRadius: 'var(--radius-lg)',
        background: 'var(--surface-sunken)',
        border: 'none',
        cursor: 'pointer',
        textAlign: 'left'
      }
    }, /*#__PURE__*/React.createElement(Avatar, {
      src: null,
      name: seller.name,
      shape: "rounded",
      size: 46,
      verified: seller.vedette
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-bold) 15px/1.2 var(--font-sans)',
        color: 'var(--text-body)'
      }
    }, seller.name), /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 3
      }
    }, /*#__PURE__*/React.createElement(StarRating, {
      value: seller.rating,
      count: seller.reviews,
      size: "sm"
    }))), /*#__PURE__*/React.createElement("i", {
      className: "ti ti-chevron-right",
      style: {
        fontSize: 22,
        color: 'var(--text-disabled)'
      }
    })), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 10
      }
    }, /*#__PURE__*/React.createElement(Button, {
      variant: "whatsapp",
      icon: "brand-whatsapp",
      size: "lg",
      fullWidth: true,
      onClick: () => nav.toast('Ouverture de WhatsApp…', {
        icon: 'brand-whatsapp'
      })
    }, "WhatsApp"), /*#__PURE__*/React.createElement(Button, {
      variant: "call",
      icon: "phone",
      size: "lg",
      onClick: () => nav.toast('Appel en cours…', {
        icon: 'phone'
      })
    }, "Appeler")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h3", {
      style: {
        font: 'var(--fw-bold) 14px/1 var(--font-sans)',
        margin: '0 0 7px',
        color: 'var(--text-body)'
      }
    }, "Description"), /*#__PURE__*/React.createElement("p", {
      style: {
        font: 'var(--fw-regular) 13.5px/1.6 var(--font-sans)',
        color: 'var(--text-muted)',
        margin: 0
      }
    }, p.desc)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 10
      }
    }, /*#__PURE__*/React.createElement("h3", {
      style: {
        font: 'var(--fw-bold) 14px/1 var(--font-sans)',
        margin: 0,
        color: 'var(--text-body)'
      }
    }, "Avis r\xE9cents"), /*#__PURE__*/React.createElement("span", {
      style: {
        font: 'var(--fw-bold) 12.5px/1 var(--font-sans)',
        color: 'var(--color-primary)'
      }
    }, "Voir tous")), [{
      n: 'Mariam K.',
      r: 5,
      t: 'Vendeur sérieux, produit conforme. Je recommande !'
    }, {
      n: 'Yao P.',
      r: 4,
      t: 'Bonne qualité, livraison rapide sur Abidjan.'
    }].map((a, i) => /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        padding: '11px 0',
        borderTop: i ? '1px solid var(--gray-100)' : 'none'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: 'var(--fw-semibold) 13px/1 var(--font-sans)',
        color: 'var(--text-body)'
      }
    }, a.n), /*#__PURE__*/React.createElement(StarRating, {
      value: a.r,
      size: "sm",
      showValue: false
    })), /*#__PURE__*/React.createElement("p", {
      style: {
        font: 'var(--fw-regular) 12.5px/1.5 var(--font-sans)',
        color: 'var(--text-muted)',
        margin: '5px 0 0'
      }
    }, a.t))))));
  }
  window.VKBuyer = {
    Accueil,
    Catalogue,
    Favoris,
    Profil,
    Produit
  };
})();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/screens-buyer.jsx", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/screens-seller.jsx
try { (() => {
// Vendeur.ci UI kit — auth + seller flow screens. Exposed on window.VKSeller.
(function () {
  const DS = () => window.VendeurCiDesignSystem_f69b94;
  const {
    StatusBar,
    AppBar,
    SectionHead
  } = window.VKUI;
  const D = window.VKData;
  const fmt = n => Number(n).toLocaleString('fr-FR').replace(/,/g, ' ');
  function Logo({
    size = 24
  }) {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        font: `var(--fw-extrabold) ${size}px/1 var(--font-sans)`,
        color: 'var(--color-primary)',
        letterSpacing: '-0.02em'
      }
    }, "vendeur", /*#__PURE__*/React.createElement("span", {
      style: {
        color: 'var(--text-body)'
      }
    }, ".ci"));
  }

  /* ---------------- AUTH — phone ---------------- */
  function AuthPhone({
    nav,
    params
  }) {
    const {
      Button,
      Input
    } = DS();
    const [num, setNum] = React.useState('');
    const ok = num.replace(/\s/g, '').length >= 10;
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement(AppBar, {
      onBack: nav.back,
      title: ""
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        padding: '8px 20px 24px'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        justifyContent: 'center',
        margin: '20px 0 28px'
      }
    }, /*#__PURE__*/React.createElement(Logo, {
      size: 28
    })), /*#__PURE__*/React.createElement("h1", {
      style: {
        font: 'var(--fw-extrabold) 26px/1.15 var(--font-sans)',
        letterSpacing: '-0.02em',
        margin: '0 0 8px',
        color: 'var(--text-body)'
      }
    }, "Entrez votre num\xE9ro"), /*#__PURE__*/React.createElement("p", {
      style: {
        font: 'var(--fw-regular) 14.5px/1.5 var(--font-sans)',
        color: 'var(--text-muted)',
        margin: '0 0 24px'
      }
    }, "Nous vous enverrons un code par SMS pour ", params?.intent === 'seller' ? 'créer votre boutique' : 'vous connecter', "."), /*#__PURE__*/React.createElement(Input, {
      label: "Num\xE9ro de t\xE9l\xE9phone",
      prefix: "+225",
      placeholder: "07 00 00 00 00",
      type: "tel",
      value: num,
      onChange: e => setNum(e.target.value)
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1
      }
    }), /*#__PURE__*/React.createElement(Button, {
      variant: "primary",
      size: "lg",
      fullWidth: true,
      disabled: !ok,
      onClick: () => nav.go('auth-otp', params)
    }, "Recevoir le code"), /*#__PURE__*/React.createElement("p", {
      style: {
        font: 'var(--fw-regular) 11.5px/1.5 var(--font-sans)',
        color: 'var(--text-disabled)',
        textAlign: 'center',
        margin: '14px 0 0'
      }
    }, "En continuant, vous acceptez les ", /*#__PURE__*/React.createElement("span", {
      style: {
        color: 'var(--color-primary)',
        fontWeight: 'var(--fw-semibold)'
      }
    }, "Conditions d'utilisation"), ".")));
  }

  /* ---------------- AUTH — OTP ---------------- */
  function AuthOtp({
    nav,
    params
  }) {
    const {
      Button
    } = DS();
    const [code, setCode] = React.useState(['', '', '', '', '', '']);
    const refs = React.useRef([]);
    const filled = code.every(c => c !== '');
    const set = (i, v) => {
      if (!/^\d?$/.test(v)) return;
      const next = [...code];
      next[i] = v;
      setCode(next);
      if (v && i < 5) refs.current[i + 1]?.focus();
    };
    const onKey = (i, e) => {
      if (e.key === 'Backspace' && !code[i] && i > 0) refs.current[i - 1]?.focus();
    };
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement(AppBar, {
      onBack: nav.back,
      title: ""
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        padding: '8px 20px 24px'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        justifyContent: 'center',
        margin: '20px 0 28px'
      }
    }, /*#__PURE__*/React.createElement(Logo, {
      size: 28
    })), /*#__PURE__*/React.createElement("h1", {
      style: {
        font: 'var(--fw-extrabold) 26px/1.15 var(--font-sans)',
        letterSpacing: '-0.02em',
        margin: '0 0 8px',
        color: 'var(--text-body)'
      }
    }, "Entrez le code"), /*#__PURE__*/React.createElement("p", {
      style: {
        font: 'var(--fw-regular) 14.5px/1.5 var(--font-sans)',
        color: 'var(--text-muted)',
        margin: '0 0 24px'
      }
    }, "Code envoy\xE9 au ", /*#__PURE__*/React.createElement("b", {
      style: {
        color: 'var(--text-body)'
      }
    }, "+225 07 00 00 00 00")), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 9,
        justifyContent: 'space-between'
      }
    }, code.map((c, i) => /*#__PURE__*/React.createElement("input", {
      key: i,
      ref: el => refs.current[i] = el,
      value: c,
      inputMode: "numeric",
      maxLength: 1,
      onChange: e => set(i, e.target.value),
      onKeyDown: e => onKey(i, e),
      style: {
        width: 46,
        height: 56,
        textAlign: 'center',
        borderRadius: 'var(--radius-md)',
        border: `1.5px solid ${c ? 'var(--color-primary)' : 'var(--border-subtle)'}`,
        background: 'var(--surface-card)',
        font: 'var(--fw-bold) 24px/1 var(--font-sans)',
        color: 'var(--text-body)',
        outline: 'none'
      }
    }))), /*#__PURE__*/React.createElement("p", {
      style: {
        font: 'var(--fw-medium) 13px/1.5 var(--font-sans)',
        color: 'var(--text-muted)',
        textAlign: 'center',
        margin: '20px 0 0'
      }
    }, "Renvoyer le code dans ", /*#__PURE__*/React.createElement("b", {
      style: {
        color: 'var(--text-body)'
      }
    }, "1:45")), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1
      }
    }), /*#__PURE__*/React.createElement(Button, {
      variant: "primary",
      size: "lg",
      fullWidth: true,
      disabled: !filled,
      onClick: () => params?.intent === 'seller' ? nav.go('onboarding') : nav.startSeller()
    }, "Valider")));
  }

  /* ---------------- ONBOARDING (boutique) — condensé ---------------- */
  function Onboarding({
    nav
  }) {
    const {
      Button,
      Input
    } = DS();
    const [step] = React.useState(1);
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement(AppBar, {
      onBack: nav.back,
      title: "Cr\xE9er ma boutique",
      right: /*#__PURE__*/React.createElement("span", {
        style: {
          font: 'var(--fw-bold) 13px/1 var(--font-sans)',
          color: 'var(--color-primary)'
        }
      }, "1/3")
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        padding: '0 16px 8px',
        flexShrink: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 6
      }
    }, [1, 2, 3].map(s => /*#__PURE__*/React.createElement("div", {
      key: s,
      style: {
        flex: 1,
        height: 5,
        borderRadius: 100,
        background: s <= step ? 'var(--color-primary)' : 'var(--gray-200)'
      }
    })))), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        padding: '14px 16px 24px',
        gap: 16,
        overflowY: 'auto'
      }
    }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
      style: {
        font: 'var(--fw-extrabold) 22px/1.2 var(--font-sans)',
        letterSpacing: '-0.02em',
        margin: '0 0 4px',
        color: 'var(--text-body)'
      }
    }, "Cr\xE9ez votre boutique"), /*#__PURE__*/React.createElement("p", {
      style: {
        font: 'var(--fw-regular) 14px/1.5 var(--font-sans)',
        color: 'var(--text-muted)',
        margin: 0
      }
    }, "Ces infos seront visibles par les acheteurs.")), /*#__PURE__*/React.createElement(Input, {
      label: "Nom de la boutique",
      placeholder: "Ex : Awa Couture"
    }), /*#__PURE__*/React.createElement(Input, {
      label: "Cat\xE9gorie principale",
      icon: "category",
      placeholder: "Mode & v\xEAtements"
    }), /*#__PURE__*/React.createElement(Input, {
      label: "Description courte",
      placeholder: "V\xEAtements wax sur mesure \xE0 Abidjan"
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1
      }
    }), /*#__PURE__*/React.createElement(Button, {
      variant: "primary",
      size: "lg",
      fullWidth: true,
      iconRight: "arrow-right",
      onClick: () => nav.startSeller()
    }, "Continuer")));
  }

  /* ---------------- DASHBOARD ---------------- */
  function Dashboard({
    nav
  }) {
    const {
      IconButton,
      StatCard,
      Badge,
      BottomNav
    } = DS();
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
      style: {
        background: 'linear-gradient(150deg, var(--blue-600), var(--blue-700))',
        borderRadius: '0 0 28px 28px',
        flexShrink: 0,
        paddingBottom: 22
      }
    }, /*#__PURE__*/React.createElement(StatusBar, {
      tone: "light"
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        padding: '6px 20px 16px',
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between'
      }
    }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-medium) 14px/1 var(--font-sans)',
        color: 'rgba(255,255,255,.85)'
      }
    }, "Bonjour,"), /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-extrabold) 22px/1.1 var(--font-sans)',
        color: '#fff',
        marginTop: 3
      }
    }, "Awa \uD83D\uDC4B"), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: 5,
        marginTop: 8,
        font: 'var(--fw-bold) 11.5px/1 var(--font-sans)',
        background: 'rgba(74,222,128,.22)',
        color: '#d6ffe5',
        padding: '5px 10px',
        borderRadius: 100
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 7,
        height: 7,
        borderRadius: '50%',
        background: '#4ade80'
      }
    }), "Boutique active \xB7 Plan Essentiel")), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 8
      }
    }, /*#__PURE__*/React.createElement(IconButton, {
      icon: "bell",
      tone: "onBlue",
      badge: true
    }), /*#__PURE__*/React.createElement(IconButton, {
      icon: "settings",
      tone: "onBlue"
    }))), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 10,
        padding: '0 20px'
      }
    }, /*#__PURE__*/React.createElement(StatCard, {
      icon: "eye",
      value: "2 847",
      label: "Vues",
      tone: "blue"
    }), /*#__PURE__*/React.createElement(StatCard, {
      icon: "brand-whatsapp",
      value: "214",
      label: "WhatsApp",
      tone: "green"
    }), /*#__PURE__*/React.createElement(StatCard, {
      icon: "phone",
      value: "68",
      label: "Appels",
      tone: "gray"
    }))), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        overflowY: 'auto',
        padding: '18px 16px 16px',
        display: 'flex',
        flexDirection: 'column',
        gap: 12
      }
    }, /*#__PURE__*/React.createElement(SectionHead, {
      title: "Mes produits",
      onAction: () => nav.stab(1)
    }), D.sellerProducts.slice(0, 3).map(p => /*#__PURE__*/React.createElement(SellerProductRow, {
      key: p.id,
      p: p,
      nav: nav
    }))), /*#__PURE__*/React.createElement(BottomNav, {
      variant: "seller",
      active: 0,
      onSelect: nav.stab,
      fab: true,
      onFab: () => nav.go('add-product')
    }));
  }

  /* ---------------- MES PRODUITS ---------------- */
  function Produits({
    nav
  }) {
    const {
      Button,
      BottomNav,
      Chip
    } = DS();
    const [filter, setFilter] = React.useState('all');
    const list = D.sellerProducts.filter(p => filter === 'all' || p.status === filter);
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement(AppBar, {
      title: "Mes produits",
      right: /*#__PURE__*/React.createElement(Button, {
        size: "sm",
        icon: "plus",
        onClick: () => nav.go('add-product')
      }, "Ajouter")
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        padding: '0 16px 12px',
        flexShrink: 0,
        display: 'flex',
        gap: 9
      }
    }, /*#__PURE__*/React.createElement(Chip, {
      selected: filter === 'all',
      onClick: () => setFilter('all')
    }, "Tous \xB7 ", D.sellerProducts.length), /*#__PURE__*/React.createElement(Chip, {
      selected: filter === 'active',
      onClick: () => setFilter('active')
    }, "Actifs"), /*#__PURE__*/React.createElement(Chip, {
      selected: filter === 'paused',
      onClick: () => setFilter('paused')
    }, "En pause")), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        overflowY: 'auto',
        padding: '0 16px 16px',
        display: 'flex',
        flexDirection: 'column',
        gap: 12
      }
    }, list.map(p => /*#__PURE__*/React.createElement(SellerProductRow, {
      key: p.id,
      p: p,
      nav: nav
    }))), /*#__PURE__*/React.createElement(BottomNav, {
      variant: "seller",
      active: 1,
      onSelect: nav.stab,
      fab: true,
      onFab: () => nav.go('add-product')
    }));
  }
  function SellerProductRow({
    p,
    nav
  }) {
    const {
      Badge
    } = DS();
    return /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        background: 'var(--surface-card)',
        borderRadius: 'var(--radius-lg)',
        padding: 11,
        boxShadow: 'var(--shadow-sm)'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        width: 56,
        height: 56,
        borderRadius: 14,
        flexShrink: 0,
        background: p.grad,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: `ti ti-${p.ghost}`,
      style: {
        fontSize: 26,
        color: 'rgba(255,255,255,.65)'
      }
    })), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-bold) 14px/1.2 var(--font-sans)',
        color: 'var(--text-body)',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis'
      }
    }, p.title), /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-extrabold) 14px/1 var(--font-sans)',
        color: 'var(--color-price)',
        marginTop: 3
      }
    }, fmt(p.price), " ", /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 10,
        opacity: 0.7
      }
    }, "FCFA")), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        marginTop: 5
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: 3,
        font: 'var(--fw-medium) 11.5px/1 var(--font-sans)',
        color: 'var(--text-disabled)'
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: "ti ti-eye",
      style: {
        fontSize: 13
      }
    }), p.views), /*#__PURE__*/React.createElement(Badge, {
      variant: p.status === 'active' ? 'active' : 'paused',
      size: "sm"
    }, p.status === 'active' ? 'Actif' : 'En pause'))), /*#__PURE__*/React.createElement("button", {
      onClick: () => nav.sheet('product-menu'),
      "aria-label": "Options",
      style: {
        border: 'none',
        background: 'none',
        cursor: 'pointer',
        color: 'var(--text-disabled)',
        padding: 4
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: "ti ti-dots-vertical",
      style: {
        fontSize: 20
      }
    })));
  }

  /* ---------------- AJOUT PRODUIT ---------------- */
  function AddProduct({
    nav
  }) {
    const {
      Button,
      Input,
      Badge
    } = DS();
    const [nego, setNego] = React.useState(true);
    const [cat, setCat] = React.useState(null);
    const [openCat, setOpenCat] = React.useState(false);
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(StatusBar, null), /*#__PURE__*/React.createElement(AppBar, {
      title: "Nouveau produit",
      right: /*#__PURE__*/React.createElement("button", {
        onClick: nav.back,
        style: {
          border: 'none',
          background: 'none',
          font: 'var(--fw-semibold) 14px/1 var(--font-sans)',
          color: 'var(--text-muted)',
          cursor: 'pointer'
        }
      }, "Annuler")
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        overflowY: 'auto',
        padding: '6px 16px 16px',
        display: 'flex',
        flexDirection: 'column',
        gap: 18
      }
    }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-semibold) 13px/1 var(--font-sans)',
        color: 'var(--text-body)',
        marginBottom: 8
      }
    }, "Photos ", /*#__PURE__*/React.createElement("span", {
      style: {
        color: 'var(--text-disabled)'
      }
    }, "(jusqu'\xE0 5)")), /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        gap: 10
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        width: 84,
        height: 84,
        borderRadius: 'var(--radius-md)',
        border: '2px dashed var(--blue-200)',
        background: 'var(--color-primary-soft)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 3,
        color: 'var(--color-primary)',
        cursor: 'pointer'
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: "ti ti-camera-plus",
      style: {
        fontSize: 24
      }
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: 'var(--fw-bold) 10px/1 var(--font-sans)'
      }
    }, "Ajouter")), /*#__PURE__*/React.createElement("div", {
      style: {
        width: 84,
        height: 84,
        borderRadius: 'var(--radius-md)',
        background: D.G.pink,
        position: 'relative'
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        position: 'absolute',
        top: 5,
        right: 5,
        width: 22,
        height: 22,
        borderRadius: '50%',
        background: 'rgba(0,0,0,.45)',
        color: '#fff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: "ti ti-x",
      style: {
        fontSize: 14
      }
    }))))), /*#__PURE__*/React.createElement(Input, {
      label: "Titre du produit",
      placeholder: "Ex : Robe wax sur mesure"
    }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-semibold) 13px/1 var(--font-sans)',
        color: 'var(--text-body)',
        marginBottom: 8
      }
    }, "Cat\xE9gorie"), /*#__PURE__*/React.createElement("button", {
      onClick: () => setOpenCat(v => !v),
      style: {
        width: '100%',
        height: 48,
        padding: '0 16px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderRadius: 'var(--radius-md)',
        border: `1.5px solid ${openCat ? 'var(--color-primary)' : 'var(--border-subtle)'}`,
        background: 'var(--surface-card)',
        cursor: 'pointer',
        font: 'var(--fw-medium) 15px/1 var(--font-sans)',
        color: cat ? 'var(--text-body)' : 'var(--text-disabled)'
      }
    }, cat ? /*#__PURE__*/React.createElement("span", {
      style: {
        display: 'flex',
        alignItems: 'center',
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: `ti ti-${cat.icon}`,
      style: {
        fontSize: 19,
        color: 'var(--color-primary)'
      }
    }), cat.label) : 'Choisir une catégorie', /*#__PURE__*/React.createElement("i", {
      className: `ti ti-chevron-${openCat ? 'up' : 'down'}`,
      style: {
        fontSize: 20,
        color: 'var(--text-muted)'
      }
    })), openCat && /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 8,
        background: 'var(--surface-card)',
        borderRadius: 'var(--radius-md)',
        boxShadow: 'var(--shadow-md)',
        overflow: 'hidden'
      }
    }, D.categories.map(c => /*#__PURE__*/React.createElement("button", {
      key: c.id,
      onClick: () => {
        setCat(c);
        setOpenCat(false);
      },
      style: {
        width: '100%',
        padding: '11px 14px',
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        border: 'none',
        borderBottom: '1px solid var(--gray-100)',
        background: 'none',
        cursor: 'pointer',
        font: 'var(--fw-medium) 14px/1 var(--font-sans)',
        color: 'var(--text-body)',
        textAlign: 'left'
      }
    }, /*#__PURE__*/React.createElement("i", {
      className: `ti ti-${c.icon}`,
      style: {
        fontSize: 19,
        color: 'var(--color-primary)'
      }
    }), c.label)))), /*#__PURE__*/React.createElement(Input, {
      label: "Prix",
      suffix: "FCFA",
      placeholder: "0",
      type: "number"
    }), /*#__PURE__*/React.createElement("button", {
      onClick: () => setNego(v => !v),
      style: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        border: 'none',
        background: 'none',
        cursor: 'pointer',
        padding: 0
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: 'var(--fw-semibold) 14px/1 var(--font-sans)',
        color: 'var(--text-body)'
      }
    }, "Prix n\xE9gociable"), /*#__PURE__*/React.createElement("span", {
      style: {
        width: 48,
        height: 28,
        borderRadius: 100,
        background: nego ? 'var(--color-primary)' : 'var(--gray-300)',
        position: 'relative',
        transition: 'background .2s'
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        position: 'absolute',
        top: 3,
        left: nego ? 23 : 3,
        width: 22,
        height: 22,
        borderRadius: '50%',
        background: '#fff',
        transition: 'left .2s',
        boxShadow: 'var(--shadow-xs)'
      }
    }))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: 'var(--fw-semibold) 13px/1 var(--font-sans)',
        color: 'var(--text-body)',
        marginBottom: 8
      }
    }, "Description ", /*#__PURE__*/React.createElement("span", {
      style: {
        color: 'var(--text-disabled)'
      }
    }, "(optionnel)")), /*#__PURE__*/React.createElement("textarea", {
      placeholder: "D\xE9crivez votre produit\u2026",
      rows: 3,
      style: {
        width: '100%',
        resize: 'none',
        padding: '12px 16px',
        borderRadius: 'var(--radius-md)',
        border: '1.5px solid var(--border-subtle)',
        background: 'var(--surface-card)',
        font: 'var(--fw-regular) 14px/1.5 var(--font-sans)',
        color: 'var(--text-body)',
        outline: 'none',
        boxSizing: 'border-box'
      }
    }))), /*#__PURE__*/React.createElement("div", {
      style: {
        flexShrink: 0,
        padding: '12px 16px',
        borderTop: '1px solid var(--gray-100)',
        background: 'var(--surface-card)'
      }
    }, /*#__PURE__*/React.createElement(Button, {
      variant: "primary",
      size: "lg",
      fullWidth: true,
      onClick: () => {
        nav.toast('Produit publié !', {
          icon: 'circle-check'
        });
        nav.go('dashboard');
      }
    }, "Publier le produit")));
  }
  window.VKSeller = {
    AuthPhone,
    AuthOtp,
    Onboarding,
    Dashboard,
    Produits,
    AddProduct
  };
})();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/screens-seller.jsx", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/ui.jsx
try { (() => {
// Vendeur.ci UI kit — shared shell pieces. Exposed on window.VKUI.
const {
  useState,
  useEffect
} = React;

/* Status bar (iOS-like). tone: 'dark' (default) | 'light' (on blue) */
function StatusBar({
  tone = 'dark'
}) {
  const color = tone === 'light' ? '#fff' : 'var(--text-body)';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: 44,
      flexShrink: 0,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px 0 28px',
      color
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--fw-bold) 14px/1 var(--font-sans)',
      letterSpacing: '0.02em'
    }
  }, "9:41"), /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'flex',
      gap: 6,
      fontSize: 15
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: "ti ti-signal-4g"
  }), /*#__PURE__*/React.createElement("i", {
    className: "ti ti-wifi"
  }), /*#__PURE__*/React.createElement("i", {
    className: "ti ti-battery-3"
  })));
}

/* Plain top app bar with optional back + title + actions */
function AppBar({
  title,
  onBack,
  right = null,
  logo = false
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      padding: '4px 14px 12px',
      flexShrink: 0
    }
  }, onBack && /*#__PURE__*/React.createElement("button", {
    onClick: onBack,
    "aria-label": "Retour",
    style: {
      width: 40,
      height: 40,
      borderRadius: 'var(--radius-md)',
      border: 'none',
      background: 'var(--surface-card)',
      boxShadow: 'var(--shadow-xs)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      cursor: 'pointer',
      flexShrink: 0
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: "ti ti-arrow-left",
    style: {
      fontSize: 22,
      color: 'var(--text-body)'
    }
  })), logo ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--fw-extrabold) 21px/1 var(--font-sans)',
      color: 'var(--color-primary)',
      letterSpacing: '-0.02em',
      flex: 1
    }
  }, "vendeur", /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--text-body)'
    }
  }, ".ci")) : /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--fw-bold) 18px/1.2 var(--font-sans)',
      color: 'var(--text-body)',
      flex: 1
    }
  }, title), right);
}

/* Section header with optional "Voir tout" */
function SectionHead({
  title,
  action = 'Voir tout',
  onAction = null
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 2px'
    }
  }, /*#__PURE__*/React.createElement("h2", {
    style: {
      font: 'var(--fw-extrabold) 17px/1 var(--font-sans)',
      color: 'var(--text-body)',
      letterSpacing: '-0.01em',
      margin: 0
    }
  }, title), onAction && /*#__PURE__*/React.createElement("button", {
    onClick: onAction,
    style: {
      border: 'none',
      background: 'none',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      gap: 2,
      font: 'var(--fw-bold) 13px/1 var(--font-sans)',
      color: 'var(--color-primary)'
    }
  }, action, " ", /*#__PURE__*/React.createElement("i", {
    className: "ti ti-chevron-right",
    style: {
      fontSize: 16
    }
  })));
}

/* Toast — auto-dismiss, slides up from bottom */
function Toast({
  message,
  icon = 'check',
  tone = 'success',
  onDone
}) {
  useEffect(() => {
    const t = setTimeout(onDone, 2200);
    return () => clearTimeout(t);
  }, []);
  const bg = tone === 'success' ? 'var(--ink-900)' : tone === 'error' ? 'var(--color-error)' : 'var(--ink-900)';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 16,
      right: 16,
      bottom: 90,
      zIndex: 60,
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      padding: '13px 16px',
      borderRadius: 'var(--radius-md)',
      background: bg,
      color: '#fff',
      boxShadow: 'var(--shadow-lg)',
      font: 'var(--fw-semibold) 13.5px/1.3 var(--font-sans)',
      animation: 'vci-fade-up var(--dur-base) var(--ease-out)'
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${icon}`,
    style: {
      fontSize: 20,
      color: tone === 'success' ? 'var(--green-500)' : '#fff'
    }
  }), message);
}

/* Bottom sheet — scrim + slide-up panel */
function BottomSheet({
  title,
  onClose,
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      zIndex: 70,
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'flex-end'
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onClose,
    style: {
      position: 'absolute',
      inset: 0,
      background: 'var(--scrim)',
      animation: 'vci-fade-up var(--dur-fast) ease'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'relative',
      background: 'var(--surface-card)',
      borderRadius: '28px 28px 0 0',
      padding: '10px 18px 24px',
      boxShadow: 'var(--shadow-lg)',
      animation: 'vci-sheet var(--dur-slow) var(--ease-out)'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 4,
      borderRadius: 100,
      background: 'var(--gray-200)',
      margin: '0 auto 14px'
    }
  }), title && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: 'var(--fw-bold) 18px/1 var(--font-sans)',
      margin: 0,
      color: 'var(--text-body)'
    }
  }, title), /*#__PURE__*/React.createElement("button", {
    onClick: onClose,
    style: {
      border: 'none',
      background: 'var(--surface-sunken)',
      width: 32,
      height: 32,
      borderRadius: '50%',
      cursor: 'pointer',
      color: 'var(--text-muted)'
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: "ti ti-x",
    style: {
      fontSize: 18
    }
  }))), children));
}

/* Empty state */
function EmptyState({
  icon,
  title,
  sub,
  cta,
  onCta
}) {
  const {
    Button
  } = window.VendeurCiDesignSystem_f69b94;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      textAlign: 'center',
      padding: 30,
      gap: 6
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 88,
      height: 88,
      borderRadius: '50%',
      background: 'var(--color-primary-soft)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      marginBottom: 10
    }
  }, /*#__PURE__*/React.createElement("i", {
    className: `ti ti-${icon}`,
    style: {
      fontSize: 42,
      color: 'var(--color-primary)'
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--fw-bold) 17px/1.3 var(--font-sans)',
      color: 'var(--text-body)'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--fw-regular) 14px/1.5 var(--font-sans)',
      color: 'var(--text-muted)',
      maxWidth: 240
    }
  }, sub), cta && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14
    }
  }, /*#__PURE__*/React.createElement(Button, {
    onClick: onCta,
    icon: "compass"
  }, cta)));
}
window.VKUI = {
  StatusBar,
  AppBar,
  SectionHead,
  Toast,
  BottomSheet,
  EmptyState
};
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/ui.jsx", error: String((e && e.message) || e) }); }

__ds_ns.ProductCard = __ds_scope.ProductCard;

__ds_ns.SellerCard = __ds_scope.SellerCard;

__ds_ns.StatCard = __ds_scope.StatCard;

__ds_ns.Avatar = __ds_scope.Avatar;

__ds_ns.Badge = __ds_scope.Badge;

__ds_ns.Button = __ds_scope.Button;

__ds_ns.Chip = __ds_scope.Chip;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.Input = __ds_scope.Input;

__ds_ns.StarRating = __ds_scope.StarRating;

__ds_ns.BottomNav = __ds_scope.BottomNav;

__ds_ns.SearchBar = __ds_scope.SearchBar;

})();
