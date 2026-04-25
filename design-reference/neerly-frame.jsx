// Neerly Android frame + icon set + small UI primitives
// Keeps a single 360×780 phone viewport for all screens.

const PHONE_W = 360;
const PHONE_H = 780;

// —————————————————————————— Icons (inline SVG) ——————————————————————————
// All icons use stroke=currentColor so you can tint with color.
const SvgIcon = ({ d, size = 20, stroke = 1.8, fill = 'none', children }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={fill}
       stroke="currentColor" strokeWidth={stroke}
       strokeLinecap="round" strokeLinejoin="round">
    {d ? <path d={d}/> : children}
  </svg>
);

const Icon = {
  Back:   (p) => <SvgIcon {...p}><path d="M14 6l-6 6 6 6"/></SvgIcon>,
  Menu:   (p) => <SvgIcon {...p}><line x1="4" y1="7" x2="20" y2="7"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="17" x2="20" y2="17"/></SvgIcon>,
  Close:  (p) => <SvgIcon {...p}><line x1="6" y1="6" x2="18" y2="18"/><line x1="18" y1="6" x2="6" y2="18"/></SvgIcon>,
  Search: (p) => <SvgIcon {...p}><circle cx="11" cy="11" r="7"/><line x1="16" y1="16" x2="21" y2="21"/></SvgIcon>,
  Bell:   (p) => <SvgIcon {...p}><path d="M6 9a6 6 0 1112 0c0 4 2 5 2 6H4c0-1 2-2 2-6z"/><path d="M10 19a2 2 0 004 0"/></SvgIcon>,
  Location:(p) => <SvgIcon {...p}><path d="M12 22s7-7 7-12a7 7 0 00-14 0c0 5 7 12 7 12z"/><circle cx="12" cy="10" r="2.5"/></SvgIcon>,
  Chevron:(p) => <SvgIcon {...p}><path d="M9 6l6 6-6 6"/></SvgIcon>,
  ChevronDown:(p) => <SvgIcon {...p}><path d="M6 9l6 6 6-6"/></SvgIcon>,
  Home:   (p) => <SvgIcon {...p}><path d="M3 11l9-7 9 7v9a2 2 0 01-2 2h-4v-6h-6v6H5a2 2 0 01-2-2z"/></SvgIcon>,
  Box:    (p) => <SvgIcon {...p}><path d="M3 7l9-4 9 4-9 4-9-4z"/><path d="M3 7v10l9 4 9-4V7"/><line x1="12" y1="11" x2="12" y2="21"/></SvgIcon>,
  Sync:   (p) => <SvgIcon {...p}><path d="M4 4v6h6"/><path d="M20 20v-6h-6"/><path d="M4 10a8 8 0 0114-3M20 14a8 8 0 01-14 3"/></SvgIcon>,
  Wallet: (p) => <SvgIcon {...p}><rect x="3" y="6" width="18" height="14" rx="3"/><path d="M17 13h2"/><path d="M3 10h18"/></SvgIcon>,
  User:   (p) => <SvgIcon {...p}><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0116 0"/></SvgIcon>,
  Phone:  (p) => <SvgIcon {...p}><path d="M5 4h4l2 5-2 1a12 12 0 006 6l1-2 5 2v4a2 2 0 01-2 2A17 17 0 013 6a2 2 0 012-2z"/></SvgIcon>,
  Plus:   (p) => <SvgIcon {...p}><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></SvgIcon>,
  Minus:  (p) => <SvgIcon {...p}><line x1="5" y1="12" x2="19" y2="12"/></SvgIcon>,
  Check:  (p) => <SvgIcon {...p}><path d="M5 12l4 4 10-10"/></SvgIcon>,
  Star:   (p) => <SvgIcon {...p} fill="currentColor" stroke="none"><path d="M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.9L12 17.8 5.8 21l1.2-6.9-5-4.9 6.9-1z"/></SvgIcon>,
  Clock:  (p) => <SvgIcon {...p}><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></SvgIcon>,
  Pin:    (p) => <SvgIcon {...p}><path d="M12 22s7-7 7-12a7 7 0 00-14 0c0 5 7 12 7 12z"/></SvgIcon>,
  Heart:  (p) => <SvgIcon {...p}><path d="M12 21s-8-5-8-11a5 5 0 019-3 5 5 0 019 3c0 6-8 11-8 11z"/></SvgIcon>,
  Share:  (p) => <SvgIcon {...p}><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="9" y1="14" x2="15" y2="18"/><line x1="15" y1="6" x2="9" y2="10"/></SvgIcon>,
  Truck:  (p) => <SvgIcon {...p}><path d="M3 7h11v10H3z"/><path d="M14 10h4l3 3v4h-7"/><circle cx="7" cy="18" r="2"/><circle cx="17" cy="18" r="2"/></SvgIcon>,
  Camera: (p) => <SvgIcon {...p}><path d="M4 8h3l2-3h6l2 3h3a1 1 0 011 1v10a1 1 0 01-1 1H4a1 1 0 01-1-1V9a1 1 0 011-1z"/><circle cx="12" cy="13" r="4"/></SvgIcon>,
  Shield: (p) => <SvgIcon {...p}><path d="M12 3l8 3v6c0 5-4 8-8 9-4-1-8-4-8-9V6z"/></SvgIcon>,
  Doc:    (p) => <SvgIcon {...p}><path d="M6 3h8l4 4v14H6z"/><path d="M14 3v4h4"/></SvgIcon>,
  Card:   (p) => <SvgIcon {...p}><rect x="3" y="6" width="18" height="12" rx="2"/><line x1="3" y1="10" x2="21" y2="10"/></SvgIcon>,
  Gear:   (p) => <SvgIcon {...p}><circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 00-.1-1l2-1.5-2-3.5-2.3.9a7 7 0 00-1.7-1L14 3h-4l-.9 2.4a7 7 0 00-1.7 1L5 5.5l-2 3.5L5 10.5a7 7 0 000 3L3 15l2 3.5 2.3-.9a7 7 0 001.7 1L10 21h4l.9-2.4a7 7 0 001.7-1l2.3.9 2-3.5-2-1.5c.1-.3.1-.7.1-1z"/></SvgIcon>,
  Dots:   (p) => <SvgIcon {...p}><circle cx="5" cy="12" r="1.6" fill="currentColor"/><circle cx="12" cy="12" r="1.6" fill="currentColor"/><circle cx="19" cy="12" r="1.6" fill="currentColor"/></SvgIcon>,
  Drop:   (p) => <SvgIcon {...p}><path d="M12 2s6 7 6 12a6 6 0 01-12 0c0-5 6-12 6-12z"/></SvgIcon>,
  Wifi:   (p) => <SvgIcon {...p}><path d="M2 9a16 16 0 0120 0"/><path d="M5 12.5a12 12 0 0114 0"/><path d="M8.5 16a7 7 0 017 0"/><circle cx="12" cy="19" r="1" fill="currentColor"/></SvgIcon>,
  Calendar:(p)=> <SvgIcon {...p}><rect x="3" y="5" width="18" height="16" rx="2"/><line x1="3" y1="10" x2="21" y2="10"/><line x1="8" y1="3" x2="8" y2="7"/><line x1="16" y1="3" x2="16" y2="7"/></SvgIcon>,
  Download:(p)=> <SvgIcon {...p}><path d="M12 4v12"/><path d="M7 11l5 5 5-5"/><path d="M5 20h14"/></SvgIcon>,
  Edit:   (p) => <SvgIcon {...p}><path d="M4 20h4l10-10-4-4L4 16z"/></SvgIcon>,
  Flag:   (p) => <SvgIcon {...p}><path d="M5 21V4"/><path d="M5 4h12l-2 4 2 4H5"/></SvgIcon>,
  Trash:  (p) => <SvgIcon {...p}><path d="M4 7h16"/><path d="M10 7V4h4v3"/><path d="M6 7l1 13h10l1-13"/></SvgIcon>,
  Alert:  (p) => <SvgIcon {...p}><path d="M12 3l10 18H2z"/><line x1="12" y1="10" x2="12" y2="14"/><circle cx="12" cy="17" r="1" fill="currentColor"/></SvgIcon>,
  Check2: (p) => <SvgIcon {...p}><circle cx="12" cy="12" r="9"/><path d="M8 12l3 3 5-5"/></SvgIcon>,
  Nav:    (p) => <SvgIcon {...p}><path d="M3 11l18-8-8 18-2-8z"/></SvgIcon>,
  Mic:    (p) => <SvgIcon {...p}><rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0014 0"/><line x1="12" y1="18" x2="12" y2="22"/></SvgIcon>,
  Copy:   (p) => <SvgIcon {...p}><rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V6a2 2 0 00-2-2H6a2 2 0 00-2 2v8a2 2 0 002 2h2"/></SvgIcon>,
  Info:   (p) => <SvgIcon {...p}><circle cx="12" cy="12" r="9"/><line x1="12" y1="10" x2="12" y2="16"/><circle cx="12" cy="7" r="1" fill="currentColor"/></SvgIcon>,
  Refresh:(p) => <SvgIcon {...p}><path d="M20 11a8 8 0 10-2 5"/><path d="M20 4v6h-6"/></SvgIcon>,
  Filter: (p) => <SvgIcon {...p}><path d="M4 5h16l-6 8v6l-4-2v-4z"/></SvgIcon>,
  List:   (p) => <SvgIcon {...p}><line x1="8" y1="6" x2="20" y2="6"/><line x1="8" y1="12" x2="20" y2="12"/><line x1="8" y1="18" x2="20" y2="18"/><circle cx="4" cy="6" r="1" fill="currentColor"/><circle cx="4" cy="12" r="1" fill="currentColor"/><circle cx="4" cy="18" r="1" fill="currentColor"/></SvgIcon>,
  Chart:  (p) => <SvgIcon {...p}><line x1="4" y1="20" x2="20" y2="20"/><rect x="6" y="12" width="3" height="8"/><rect x="11" y="8" width="3" height="12"/><rect x="16" y="14" width="3" height="6"/></SvgIcon>,
  Balance:(p) => <SvgIcon {...p}><path d="M12 3v18"/><path d="M6 21h12"/><path d="M6 9l-3 6a3 3 0 006 0z"/><path d="M18 9l-3 6a3 3 0 006 0z"/><path d="M3 6h18"/></SvgIcon>,
  Scale:  (p) => <SvgIcon {...p}><path d="M4 20h16"/><path d="M8 20V6a2 2 0 012-2h4a2 2 0 012 2v14"/></SvgIcon>,
  Gift:   (p) => <SvgIcon {...p}><rect x="3" y="8" width="18" height="13" rx="2"/><line x1="12" y1="8" x2="12" y2="21"/><path d="M7 8a3 3 0 015-3 3 3 0 015 3"/></SvgIcon>,
  Pause:  (p) => <SvgIcon {...p}><rect x="7" y="5" width="3" height="14"/><rect x="14" y="5" width="3" height="14"/></SvgIcon>,
  Skip:   (p) => <SvgIcon {...p}><path d="M6 5l8 7-8 7z"/><line x1="17" y1="5" x2="17" y2="19"/></SvgIcon>,
};

// —————————————————————————— Status bar ——————————————————————————
function AndroidStatusBar({ time = '9:30', dark = false, bg }) {
  const color = dark ? '#fff' : 'var(--ink-900)';
  return (
    <div className="android-statusbar" style={{ background: bg || 'transparent', color }}>
      <div style={{ fontWeight: 600 }}>{time}</div>
      <div className="camera" />
      <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
        <Icon.Wifi size={14}/>
        <svg width="18" height="12" viewBox="0 0 18 12" fill="none" stroke="currentColor" strokeWidth="1.6">
          <rect x="0.5" y="0.5" width="14" height="11" rx="2"/>
          <rect x="16" y="4" width="1.5" height="4" rx="0.5" fill="currentColor" stroke="none"/>
          <rect x="2" y="2" width="10" height="8" fill="currentColor" stroke="none"/>
        </svg>
      </div>
    </div>
  );
}

// —————————————————————————— Nav gesture bar ——————————————————————————
function AndroidNavBar({ dark = false, bg }) {
  return (
    <div className="android-navbar" style={{ background: bg || 'transparent' }}>
      <div style={{
        width: 108, height: 4, borderRadius: 2,
        background: dark ? '#fff' : 'var(--ink-900)', opacity: dark ? 0.9 : 0.75,
      }}/>
    </div>
  );
}

// —————————————————————————— Phone shell ——————————————————————————
function Phone({ children, dark = false, statusTime = '9:30', statusBg, navBg }) {
  // Full 360 × 780 screen (status 28 + nav 18 included inside)
  return (
    <div style={{
      width: PHONE_W, height: PHONE_H,
      borderRadius: 36,
      background: dark ? '#0E1A24' : '#fff',
      border: '10px solid #1d2027',
      boxShadow: 'var(--shadow-phone)',
      overflow: 'hidden',
      display: 'flex', flexDirection: 'column',
      position: 'relative',
    }}>
      <AndroidStatusBar time={statusTime} dark={dark} bg={statusBg}/>
      <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column', background: dark ? '#0E1A24' : '#fff' }}>
        {children}
      </div>
      <AndroidNavBar dark={dark} bg={navBg}/>
    </div>
  );
}

// —————————————————————————— Top app bar ——————————————————————————
function TopBar({ title, back = true, trailing, subtitle, onBg }) {
  return (
    <div className="topbar" style={{ background: onBg || 'var(--paper)' }}>
      {back && <div className="icon-btn"><Icon.Back size={22}/></div>}
      <div className="grow col">
        <div className="title" style={{ lineHeight: 1.1 }}>{title}</div>
        {subtitle && <div className="t-meta" style={{ marginTop: 2 }}>{subtitle}</div>}
      </div>
      {trailing}
    </div>
  );
}

// —————————————————————————— Tab bar ——————————————————————————
function TabBar({ role = 'cust', active, items }) {
  return (
    <div className={`tabbar tabbar--${role}`}>
      {items.map((it, i) => (
        <div key={i} className={`tab ${it.key === active ? 'is-active' : ''}`}>
          <div className="ic">{it.icon}</div>
          <div>{it.label}</div>
          <div className="dot-ind"/>
        </div>
      ))}
    </div>
  );
}

const CUST_TABS = [
  { key: 'home',   label: 'Home',    icon: <Icon.Home size={22}/> },
  { key: 'orders', label: 'Orders',  icon: <Icon.Box size={22}/> },
  { key: 'subs',   label: 'Subs',    icon: <Icon.Sync size={22}/> },
  { key: 'wallet', label: 'Wallet',  icon: <Icon.Wallet size={22}/> },
  { key: 'prof',   label: 'Profile', icon: <Icon.User size={22}/> },
];

const VEND_TABS = [
  { key: 'dash',   label: 'Dashboard', icon: <Icon.Chart size={22}/> },
  { key: 'orders', label: 'Orders',    icon: <Icon.Box size={22}/> },
  { key: 'cat',    label: 'Catalog',   icon: <Icon.List size={22}/> },
  { key: 'earn',   label: 'Earn',      icon: <Icon.Wallet size={22}/> },
  { key: 'more',   label: 'More',      icon: <Icon.Dots size={22}/> },
];

const DRIV_TABS = [
  { key: 'today',  label: 'Today',   icon: <Icon.Truck size={22}/> },
  { key: 'hist',   label: 'History', icon: <Icon.Clock size={22}/> },
  { key: 'prof',   label: 'Profile', icon: <Icon.User size={22}/> },
];

const ADMN_TABS = [
  { key: 'over',   label: 'Overview',  icon: <Icon.Chart size={22}/> },
  { key: 'vend',   label: 'Vendors',   icon: <Icon.Scale size={22}/> },
  { key: 'disp',   label: 'Disputes',  icon: <Icon.Alert size={22}/> },
  { key: 'more',   label: 'More',      icon: <Icon.Dots size={22}/> },
];

// —————————————————————————— Small utilities ——————————————————————————
function Avatar({ initials, bg = 'var(--ink-200)', color = 'var(--ink-800)', size = 36 }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%',
      background: bg, color, fontWeight: 700, fontSize: size * 0.38,
      display: 'grid', placeItems: 'center', flexShrink: 0,
    }}>{initials}</div>
  );
}

function LogoMark({ size = 36, bg = 'var(--cust)' }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: size * 0.28,
      background: bg, display: 'grid', placeItems: 'center',
      color: '#fff', flexShrink: 0,
    }}>
      <svg width={size * 0.52} height={size * 0.52} viewBox="0 0 24 24" fill="#fff">
        <path d="M12 2s6 7 6 12a6 6 0 01-12 0c0-5 6-12 6-12z" />
      </svg>
    </div>
  );
}

// Caption below an artboard (used to label each frame on the canvas)
function Caption({ id, title, note }) {
  return (
    <div className="artboard-caption" style={{
      display: 'flex', flexDirection: 'column', gap: 2, maxWidth: PHONE_W + 60,
    }}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'baseline' }}>
        <span className="mono" style={{ color: 'var(--ink-400)' }}>{id}</span>
        <span style={{ color: 'var(--ink-900)', textTransform: 'none', letterSpacing: 0, fontSize: 13 }}>{title}</span>
      </div>
      {note && <div style={{ textTransform: 'none', letterSpacing: 0, color: 'var(--ink-500)', fontSize: 12 }}>{note}</div>}
    </div>
  );
}

Object.assign(window, {
  Phone, AndroidStatusBar, AndroidNavBar, TopBar, TabBar,
  CUST_TABS, VEND_TABS, DRIV_TABS, ADMN_TABS,
  Icon, Avatar, LogoMark, Caption,
  PHONE_W, PHONE_H,
});
