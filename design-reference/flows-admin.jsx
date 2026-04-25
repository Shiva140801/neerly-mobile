// Neerly — Admin (mobile) flows

// A-DASH-01 Overview
function AdminDashboard() {
  return (
    <Phone>
      <div style={{ padding: '14px 16px 12px', background: '#fff', borderBottom: '1px solid var(--ink-100)' }}>
        <div className="row row--sb">
          <div className="col" style={{ lineHeight: 1.2 }}>
            <div className="t-meta">OVERVIEW · HYDERABAD</div>
            <div className="h-lg" style={{ marginTop: 2 }}>City pulse</div>
          </div>
          <div className="pill pill--admn" style={{ fontSize: 11 }}>Live</div>
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: 12 }}>
        {/* KPI cards */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {[
            { k: 'GMV TODAY', v: '₹2.4L', d: '+12% vs yst', c: 'var(--ok)' },
            { k: 'ACTIVE ORDERS', v: '284', d: '12 at risk', c: 'var(--warn)' },
            { k: 'ONLINE VENDORS', v: '74 / 92', d: '81% uptime', c: 'var(--ink-700)' },
            { k: 'DISPUTES OPEN', v: '6', d: '2 SLA breach', c: 'var(--err)' },
          ].map((k, i) => (
            <div key={i} className="card" style={{ padding: 12 }}>
              <div style={{ fontSize: 10, letterSpacing: 0.5, color: 'var(--ink-500)', fontWeight: 600 }}>{k.k}</div>
              <div style={{ fontSize: 22, fontWeight: 800, marginTop: 4 }}>{k.v}</div>
              <div style={{ fontSize: 11, color: k.c, fontWeight: 600, marginTop: 2 }}>{k.d}</div>
            </div>
          ))}
        </div>

        {/* Order heatline */}
        <div className="card mt-12" style={{ padding: 14 }}>
          <div className="row row--sb mb-8">
            <div className="t-label">ORDERS BY HOUR</div>
            <div className="t-meta">Today</div>
          </div>
          <svg viewBox="0 0 280 60" width="100%" height="60">
            <path d="M0 50 Q 30 40 60 38 Q 90 36 120 22 Q 150 14 180 18 Q 210 24 240 12 Q 260 6 280 20" stroke="var(--admn)" strokeWidth="2.5" fill="none"/>
            <path d="M0 50 Q 30 40 60 38 Q 90 36 120 22 Q 150 14 180 18 Q 210 24 240 12 Q 260 6 280 20 L 280 60 L 0 60 Z" fill="var(--admn-soft)" opacity="0.6"/>
            <circle cx="240" cy="12" r="4" fill="var(--admn)"/>
          </svg>
          <div className="row row--sb mt-4 t-meta">
            <span>6 AM</span><span>12 PM</span><span>6 PM</span><span>Now</span>
          </div>
        </div>

        {/* Attention */}
        <div className="t-label mt-16 mb-8">NEEDS ATTENTION</div>
        <div className="col gap-8">
          {[
            { t: 'Pure Drops · KYC docs uploaded', sub: '2 hrs ago · awaiting review', type: 'kyc', color: 'var(--admn)' },
            { t: 'Dispute #D-0241 · SLA breach', sub: 'Response due 15 min ago', type: 'disp', color: 'var(--err)' },
            { t: 'Banjara Hills · surge hit 1.4×', sub: '38 orders, 4 drivers online', type: 'surge', color: 'var(--warn)' },
          ].map((a, i) => (
            <div key={i} className="card row gap-10" style={{ padding: 12, borderLeft: `3px solid ${a.color}` }}>
              <div style={{
                width: 32, height: 32, borderRadius: 10,
                background: a.type === 'kyc' ? 'var(--admn-soft)' : a.type === 'disp' ? 'var(--err-soft)' : 'var(--warn-soft)',
                color: a.color, display: 'grid', placeItems: 'center',
              }}>
                {a.type === 'kyc' ? <Icon.Doc size={16}/> : a.type === 'disp' ? <Icon.Alert size={16}/> : <Icon.Chart size={16}/>}
              </div>
              <div className="grow">
                <div style={{ fontWeight: 600, fontSize: 13 }}>{a.t}</div>
                <div className="t-meta">{a.sub}</div>
              </div>
              <Icon.Chevron size={16}/>
            </div>
          ))}
        </div>
      </div>

      <TabBar role="admn" active="over" items={ADMN_TABS}/>
    </Phone>
  );
}

// A-VEN-VERIFY Vendor verification
function AdminVendorVerify() {
  return (
    <Phone>
      <TopBar title="Verify vendor" subtitle="Queue · 4 pending"/>
      <div style={{ flex: 1, overflow: 'auto' }}>
        {/* Summary card */}
        <div style={{ padding: 16 }}>
          <div className="card" style={{ padding: 14 }}>
            <div className="row gap-10">
              <div className="placeholder" style={{ width: 48, height: 48, fontSize: 8 }}>LOGO</div>
              <div className="grow">
                <div style={{ fontWeight: 700, fontSize: 15 }}>Pure Drops Suppliers</div>
                <div className="t-meta">Madhapur · applied 2 days ago</div>
                <div className="row mt-6 gap-6">
                  <div className="pill pill--admn" style={{ fontSize: 10 }}>Tier-1 application</div>
                  <div className="pill" style={{ fontSize: 10, background: 'var(--ink-100)', color: 'var(--ink-700)' }}>Bottle + jar</div>
                </div>
              </div>
            </div>
            <div className="hr"/>
            <div className="row row--sb t-meta">
              <span>Owner · <strong style={{ color: 'var(--ink-900)' }}>Krishna Rao</strong></span>
              <span>+91 98••• ••321</span>
            </div>
          </div>
        </div>

        {/* Document checklist */}
        <div className="t-label" style={{ padding: '0 16px 8px' }}>DOCUMENT CHECKLIST</div>
        <div style={{ padding: '0 16px 8px' }} className="col gap-8">
          {[
            { name: 'FSSAI certificate', status: 'Verified', ok: true, id: 'FSSAI 10019011000256' },
            { name: 'GSTIN', status: 'Verified', ok: true, id: '36ABCDE1234F1Z5' },
            { name: 'Water quality test report', status: 'Review needed', ok: null, id: 'Sample dated 11 Apr' },
            { name: 'Plant photos (3)', status: 'Verified', ok: true, id: '3 files · 8 MB' },
            { name: 'Owner Aadhaar', status: 'Flagged', ok: false, id: 'Name mismatch' },
          ].map((d, i) => (
            <div key={i} className="card row gap-10" style={{ padding: 12 }}>
              <div style={{
                width: 36, height: 36, borderRadius: 10, display: 'grid', placeItems: 'center',
                background: d.ok === true ? 'var(--ok-soft)' : d.ok === false ? 'var(--err-soft)' : 'var(--warn-soft)',
                color: d.ok === true ? 'var(--ok)' : d.ok === false ? 'var(--err)' : 'var(--warn)',
              }}>
                {d.ok === true ? <Icon.Check size={18} stroke={2.4}/> : d.ok === false ? <Icon.Alert size={18}/> : <Icon.Info size={18}/>}
              </div>
              <div className="grow">
                <div style={{ fontWeight: 600, fontSize: 13 }}>{d.name}</div>
                <div className="t-meta">{d.id}</div>
              </div>
              <div className="pill" style={{ fontSize: 10,
                background: d.ok === true ? 'var(--ok-soft)' : d.ok === false ? 'var(--err-soft)' : 'var(--warn-soft)',
                color: d.ok === true ? 'var(--ok)' : d.ok === false ? 'var(--err)' : 'var(--warn)',
              }}>{d.status}</div>
            </div>
          ))}
        </div>

        {/* Water test preview */}
        <div style={{ padding: 16 }}>
          <div className="t-label mb-8">WATER QUALITY REPORT · PENDING REVIEW</div>
          <div className="card">
            <div className="row row--sb t-meta">
              <span>Test · TDS, pH, microbiological</span>
              <span>11 Apr 2026</span>
            </div>
            <div className="hr"/>
            <div className="row row--sb" style={{ fontSize: 13 }}>
              <span>TDS</span><span style={{ fontWeight: 700, color: 'var(--ok)' }}>138 ppm · Pass</span>
            </div>
            <div className="row row--sb mt-4" style={{ fontSize: 13 }}>
              <span>pH</span><span style={{ fontWeight: 700, color: 'var(--ok)' }}>7.1 · Pass</span>
            </div>
            <div className="row row--sb mt-4" style={{ fontSize: 13 }}>
              <span>Coliform</span><span style={{ fontWeight: 700, color: 'var(--warn)' }}>&lt; 2 CFU · Borderline</span>
            </div>
          </div>
        </div>
      </div>

      <div style={{ padding: 12, borderTop: '1px solid var(--ink-100)', background: '#fff', display: 'flex', gap: 10 }}>
        <button className="btn btn--err" style={{ flex: 1 }}>Reject</button>
        <button className="btn btn--outline" style={{ flex: 1 }}>Request info</button>
        <button className="btn btn--admn" style={{ flex: 1 }}>Approve</button>
      </div>
    </Phone>
  );
}

// A-DISP-01 Dispute detail
function AdminDispute() {
  return (
    <Phone>
      <TopBar title="Dispute #D-0241" subtitle="SLA breach · 15 min overdue" trailing={<div className="pill pill--err" style={{ fontSize: 10 }}>Open</div>}/>
      <div style={{ flex: 1, overflow: 'auto' }}>
        {/* Summary */}
        <div style={{ padding: 16 }}>
          <div className="card" style={{ background: 'var(--err-soft)', border: '1px solid var(--err)', padding: 14 }}>
            <div className="row gap-8">
              <Icon.Alert size={18}/>
              <div style={{ fontWeight: 700, color: 'var(--err)' }}>Customer reports wet/leaking jar</div>
            </div>
            <div style={{ fontSize: 12, color: 'var(--err)', marginTop: 6, lineHeight: 1.5 }}>
              Refund requested ₹340 · Order #NEE-041 · 22 Apr, 8:14 AM
            </div>
          </div>

          {/* Parties */}
          <div className="t-label mt-16 mb-8">PARTIES</div>
          <div className="col gap-8">
            {[
              { role: 'Customer', name: 'Priya M.', meta: '4.9 · 32 orders', color: 'var(--cust)' },
              { role: 'Vendor', name: 'Sri Balaji Water', meta: 'Tier-1 · 4.7 rating', color: 'var(--vend)' },
              { role: 'Driver', name: 'Ramesh K.', meta: 'NEE-DRV-204 · 4.9', color: 'var(--driv)' },
            ].map((p, i) => (
              <div key={i} className="card row gap-10" style={{ padding: 10 }}>
                <div style={{ width: 36, height: 36, borderRadius: 18, background: p.color, color: '#fff', display: 'grid', placeItems: 'center', fontWeight: 800 }}>{p.name[0]}</div>
                <div className="grow">
                  <div className="t-meta">{p.role}</div>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.name}</div>
                  <div className="t-meta">{p.meta}</div>
                </div>
                <div className="icon-btn"><Icon.Phone size={18}/></div>
              </div>
            ))}
          </div>

          {/* Evidence */}
          <div className="t-label mt-16 mb-8">EVIDENCE</div>
          <div className="row gap-8" style={{ overflowX: 'auto' }}>
            {[1, 2, 3].map((n) => (
              <div key={n} className="placeholder--photo placeholder" style={{ width: 100, height: 120, flexShrink: 0, fontSize: 10 }}>PHOTO {n}</div>
            ))}
          </div>

          {/* Timeline */}
          <div className="t-label mt-16 mb-8">TIMELINE</div>
          <div className="card">
            {[
              { t: '8:12 AM', m: 'Order delivered · OTP verified', by: 'Driver' },
              { t: '8:14 AM', m: 'Customer flagged "leaking jar"', by: 'Customer' },
              { t: '8:20 AM', m: 'Vendor notified · no response yet', by: 'System', warn: true },
              { t: '8:35 AM', m: 'Auto-escalated to admin', by: 'System' },
            ].map((e, i) => (
              <div key={i} style={{ display: 'flex', gap: 10, paddingBottom: 10, marginBottom: 10, borderBottom: i < 3 ? '1px dashed var(--ink-200)' : 0 }}>
                <div className="mono t-meta" style={{ width: 56, flexShrink: 0 }}>{e.t}</div>
                <div className="grow">
                  <div style={{ fontSize: 13, fontWeight: 600, color: e.warn ? 'var(--warn)' : 'var(--ink-900)' }}>{e.m}</div>
                  <div className="t-meta">{e.by}</div>
                </div>
              </div>
            ))}
          </div>

          {/* Quick resolutions */}
          <div className="t-label mt-16 mb-8">RESOLVE</div>
          <div className="col gap-8">
            <div className="opt-card is-active" style={{ padding: 12 }}>
              <div className="radio"/>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>Full refund ₹340 to customer</div>
                <div className="t-meta">Charged back to vendor · 1 demerit point</div>
              </div>
            </div>
            <div className="opt-card" style={{ padding: 12 }}>
              <div className="radio"/>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>Partial refund ₹170</div>
                <div className="t-meta">Split 50/50 · no vendor demerit</div>
              </div>
            </div>
            <div className="opt-card" style={{ padding: 12 }}>
              <div className="radio"/>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>Reject · insufficient evidence</div>
                <div className="t-meta">Notify customer, close ticket</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div style={{ padding: 12, borderTop: '1px solid var(--ink-100)', background: '#fff' }}>
        <button className="btn btn--admn btn--full">Apply resolution →</button>
      </div>
    </Phone>
  );
}

// A-SURGE-01 Surge control
function AdminSurge() {
  return (
    <Phone>
      <TopBar title="Surge control" subtitle="Hyderabad · live"/>
      <div style={{ flex: 1, overflow: 'auto' }}>
        {/* Heatmap */}
        <div style={{ height: 220, background: '#EAF4F3', position: 'relative', overflow: 'hidden' }}>
          <svg viewBox="0 0 360 220" width="100%" height="100%" preserveAspectRatio="none">
            <path d="M0 100 Q 90 60 180 100 T 360 90" stroke="#B8D4D0" strokeWidth="10" fill="none"/>
            <path d="M120 0 L 130 220" stroke="#B8D4D0" strokeWidth="8"/>
            <path d="M240 0 L 250 220" stroke="#B8D4D0" strokeWidth="8"/>
            {/* Heat blobs */}
            <circle cx="100" cy="80" r="56" fill="var(--err)" opacity="0.35"/>
            <circle cx="100" cy="80" r="30" fill="var(--err)" opacity="0.5"/>
            <circle cx="220" cy="140" r="44" fill="var(--warn)" opacity="0.35"/>
            <circle cx="220" cy="140" r="22" fill="var(--warn)" opacity="0.55"/>
            <circle cx="300" cy="60" r="36" fill="var(--ok)" opacity="0.35"/>
          </svg>
          <div style={{ position: 'absolute', bottom: 10, left: 10, right: 10, display: 'flex', justifyContent: 'center', gap: 6 }}>
            <div className="pill" style={{ background: 'var(--err)', color: '#fff', fontSize: 10 }}>● Banjara 1.4×</div>
            <div className="pill" style={{ background: 'var(--warn)', color: '#fff', fontSize: 10 }}>● Jubilee 1.2×</div>
            <div className="pill" style={{ background: 'var(--ok)', color: '#fff', fontSize: 10 }}>● Madhapur 1.0×</div>
          </div>
        </div>

        <div style={{ padding: 16 }}>
          <div className="t-label mb-8">ZONES · 3 ACTIVE MULTIPLIERS</div>
          <div className="col gap-10">
            {[
              { zone: 'Banjara Hills', mult: 1.4, orders: 38, drivers: 4, drivers_needed: 9 },
              { zone: 'Jubilee Hills', mult: 1.2, orders: 22, drivers: 6, drivers_needed: 8 },
              { zone: 'Madhapur', mult: 1.0, orders: 14, drivers: 11, drivers_needed: 8 },
            ].map((z, i) => (
              <div key={i} className="card" style={{ padding: 14 }}>
                <div className="row row--sb">
                  <div style={{ fontWeight: 700, fontSize: 15 }}>{z.zone}</div>
                  <div className="pill" style={{
                    fontSize: 11, fontWeight: 800,
                    background: z.mult >= 1.4 ? 'var(--err)' : z.mult > 1.0 ? 'var(--warn)' : 'var(--ok)', color: '#fff',
                  }}>{z.mult.toFixed(1)}×</div>
                </div>
                <div className="row row--sb mt-6 t-meta">
                  <span>Active orders</span><span style={{ fontWeight: 700, color: 'var(--ink-900)' }}>{z.orders}</span>
                </div>
                <div className="row row--sb mt-2 t-meta">
                  <span>Drivers online</span>
                  <span style={{ fontWeight: 700, color: z.drivers < z.drivers_needed ? 'var(--err)' : 'var(--ok)' }}>
                    {z.drivers} / {z.drivers_needed} needed
                  </span>
                </div>
                <div className="hr"/>
                {/* Slider */}
                <div className="t-label mb-8">MULTIPLIER</div>
                <div style={{ position: 'relative', height: 20 }}>
                  <div style={{ position: 'absolute', inset: '8px 0', background: 'var(--ink-100)', borderRadius: 999 }}/>
                  <div style={{
                    position: 'absolute', top: 8, left: 0, bottom: 8,
                    width: `${(z.mult - 1) * 100 + 15}%`, background: z.mult > 1 ? 'var(--warn)' : 'var(--ink-300)', borderRadius: 999,
                  }}/>
                  <div style={{
                    position: 'absolute', top: 2, left: `calc(${(z.mult - 1) * 100 + 10}% - 8px)`,
                    width: 16, height: 16, borderRadius: 8, background: '#fff', boxShadow: 'var(--shadow-1)', border: '2px solid var(--ink-300)',
                  }}/>
                </div>
                <div className="row row--sb mt-8" style={{ fontSize: 10, color: 'var(--ink-500)' }}>
                  <span>1.0×</span><span>1.5×</span><span>2.0×</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div style={{ padding: 12, borderTop: '1px solid var(--ink-100)', background: '#fff' }}>
        <button className="btn btn--admn btn--full">Apply changes</button>
      </div>
    </Phone>
  );
}

Object.assign(window, { AdminDashboard, AdminVendorVerify, AdminDispute, AdminSurge });
