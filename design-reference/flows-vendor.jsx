// Neerly — Vendor flows

// V-DASH-01
function VendorDashboard() {
  return (
    <Phone>
      {/* Top */}
      <div style={{ padding: '12px 16px 14px', background: 'var(--vend-dark)', color: '#fff' }}>
        <div className="row">
          <div className="col grow" style={{ lineHeight: 1.1 }}>
            <div style={{ fontSize: 11, opacity: 0.85, letterSpacing: 0.5 }}>GOOD MORNING, SRI BALAJI</div>
            <div style={{ fontWeight: 700, fontSize: 18, marginTop: 4 }}>Wednesday, Apr 22</div>
          </div>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 8,
            background: 'rgba(255,255,255,0.15)', padding: '6px 12px',
            borderRadius: 999, fontSize: 12, fontWeight: 600,
          }}>
            <span className="live-dot" style={{ background: '#4ADE80' }}/> Open
          </div>
        </div>

        <div className="row mt-16" style={{ gap: 10 }}>
          {[
            { k: 'Today', v: '₹4,820' },
            { k: 'Orders', v: '38' },
            { k: 'Rating', v: '4.7' },
          ].map((s, i) => (
            <div key={i} style={{
              flex: 1, background: 'rgba(255,255,255,0.12)', padding: 10,
              borderRadius: 12, backdropFilter: 'blur(4px)',
            }}>
              <div style={{ fontSize: 10, opacity: 0.8, letterSpacing: 0.4 }}>{s.k.toUpperCase()}</div>
              <div style={{ fontSize: 20, fontWeight: 800, marginTop: 2 }}>{s.v}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '14px 16px 16px' }}>
        {/* Active orders */}
        <div className="row row--sb mb-8">
          <div className="h-md">Active orders</div>
          <div className="pill pill--vend" style={{ fontSize: 11 }}>4 in progress</div>
        </div>

        <div className="col gap-10">
          {[
            { id: 'NEE-042', cust: 'Shiva R.', items: '2× 20L + 1× 1L pack', amt: 340, status: 'Preparing', sub: 'Driver arriving in 3 min', color: 'var(--warn)' },
            { id: 'NEE-041', cust: 'Priya M.', items: '1× 20L Mineral', amt: 95, status: 'Dispatched', sub: 'Ramesh · 8 min out', color: 'var(--cust)' },
            { id: 'NEE-040', cust: 'Rahul V.', items: '4× 20L Plain', amt: 240, status: 'New', sub: 'Accept within 2:04', color: 'var(--err)', isNew: true },
          ].map((o) => (
            <div key={o.id} className="card" style={{ padding: 12, border: o.isNew ? '2px solid var(--err)' : '1px solid var(--ink-200)' }}>
              <div className="row row--sb">
                <div className="col" style={{ lineHeight: 1.2 }}>
                  <div className="mono" style={{ fontSize: 11, color: 'var(--ink-500)' }}>#{o.id}</div>
                  <div style={{ fontWeight: 700, fontSize: 14, marginTop: 2 }}>{o.cust}</div>
                </div>
                <div style={{ fontWeight: 800, fontSize: 16 }}>₹{o.amt}</div>
              </div>
              <div className="t-meta" style={{ marginTop: 6 }}>{o.items}</div>
              <div className="hr" style={{ margin: '10px 0' }}/>
              <div className="row row--sb">
                <div className="row gap-6">
                  <div style={{ width: 8, height: 8, borderRadius: 4, background: o.color }}/>
                  <div style={{ fontWeight: 700, fontSize: 12, color: o.color }}>{o.status}</div>
                  <div className="t-meta">· {o.sub}</div>
                </div>
                {o.isNew && <div className="pill" style={{ fontSize: 11, background: 'var(--err)', color: '#fff' }}>Accept</div>}
              </div>
            </div>
          ))}
        </div>

        {/* Quick actions */}
        <div className="t-label mt-20 mb-8">QUICK ACTIONS</div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {[
            { label: 'Add product', icon: <Icon.Plus size={18}/> },
            { label: 'Pause shop', icon: <Icon.Pause size={18}/> },
            { label: "Today's subs", icon: <Icon.Sync size={18}/>, badge: '12' },
            { label: 'Deposits held', icon: <Icon.Balance size={18}/>, sub: '₹12,400' },
          ].map((a, i) => (
            <div key={i} className="card" style={{ padding: 12, position: 'relative' }}>
              <div style={{ color: 'var(--vend)' }}>{a.icon}</div>
              <div style={{ fontWeight: 600, fontSize: 13, marginTop: 10 }}>{a.label}</div>
              {a.sub && <div className="t-meta" style={{ marginTop: 2 }}>{a.sub}</div>}
              {a.badge && <div className="pill pill--vend" style={{ position: 'absolute', top: 10, right: 10, fontSize: 10, padding: '2px 8px' }}>{a.badge}</div>}
            </div>
          ))}
        </div>
      </div>

      <TabBar role="vend" active="dash" items={VEND_TABS}/>
    </Phone>
  );
}

// V-ORD-MODAL — Incoming order bottom sheet
function VendorIncoming() {
  return (
    <Phone>
      {/* Dim dashboard */}
      <div style={{ flex: 1, background: 'var(--ink-100)', position: 'relative' }}>
        <div style={{ position: 'absolute', inset: 0, background: 'rgba(14,26,36,0.55)' }}/>

        {/* Sheet */}
        <div style={{
          position: 'absolute', left: 0, right: 0, bottom: 0,
          background: '#fff', borderTopLeftRadius: 24, borderTopRightRadius: 24,
          padding: 18, boxShadow: 'var(--shadow-3)',
        }}>
          <div style={{ width: 44, height: 4, borderRadius: 2, background: 'var(--ink-200)', margin: '0 auto 14px' }}/>

          <div className="row gap-8">
            <div style={{ width: 8, height: 8, borderRadius: 4, background: 'var(--err)', animation: 'live-pulse 1.2s infinite' }}/>
            <div className="t-label" style={{ color: 'var(--err)' }}>NEW ORDER · ACCEPT IN</div>
            <div style={{ marginLeft: 'auto', fontFamily: 'JetBrains Mono', fontWeight: 700, fontSize: 18, color: 'var(--err)' }}>1:42</div>
          </div>

          <div className="row mt-12 gap-10">
            <Avatar initials="R" bg="var(--cust-soft)" color="var(--cust-dark)" size={44}/>
            <div className="col grow">
              <div style={{ fontWeight: 700, fontSize: 15 }}>Rahul V.</div>
              <div className="t-meta">Jubilee Hills · 2.3 km away</div>
            </div>
            <div className="pill pill--ok" style={{ fontSize: 11 }}>★ 4.8 · 12 orders</div>
          </div>

          <div className="hr"/>
          <div className="t-label">ITEMS · 2</div>
          <div className="col gap-8" style={{ marginTop: 8 }}>
            <div className="row row--sb" style={{ fontSize: 13 }}>
              <span style={{ color: 'var(--ink-800)', fontWeight: 600 }}>4× 20L Plain Jar</span>
              <span style={{ fontWeight: 700 }}>₹240</span>
            </div>
            <div className="row row--sb" style={{ fontSize: 13 }}>
              <span style={{ color: 'var(--ink-800)', fontWeight: 600 }}>1× Mineral 1L × 12</span>
              <span style={{ fontWeight: 700 }}>₹220</span>
            </div>
          </div>

          <div className="card--flat card" style={{ marginTop: 12, padding: 12 }}>
            <div className="row row--sb" style={{ fontSize: 13 }}>
              <span className="t-meta">Order total</span>
              <span style={{ fontWeight: 800, fontSize: 18 }}>₹460</span>
            </div>
            <div className="row row--sb mt-4" style={{ fontSize: 12 }}>
              <span className="t-meta">You earn (after fee)</span>
              <span style={{ color: 'var(--ok)', fontWeight: 700 }}>₹414</span>
            </div>
          </div>

          <div className="row gap-10 mt-16">
            <button className="btn btn--ghost" style={{ flex: 1 }}>Decline</button>
            <button className="btn btn--vend" style={{ flex: 2 }}>Accept order →</button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// V-ORD-DETAIL
function VendorOrderDetail() {
  return (
    <Phone>
      <TopBar title="Order #NEE-042" subtitle="Preparing" trailing={<div className="icon-btn"><Icon.Dots size={22}/></div>}/>

      <div style={{ flex: 1, overflow: 'auto' }}>
        {/* Stepper */}
        <div style={{ padding: '14px 16px', background: 'var(--vend-softer)' }}>
          <div className="row" style={{ justifyContent: 'space-between' }}>
            {['Accepted', 'Preparing', 'Ready', 'Picked up', 'Delivered'].map((s, i) => (
              <div key={s} className="col" style={{ alignItems: 'center', flex: 1, gap: 4 }}>
                <div style={{
                  width: 14, height: 14, borderRadius: 7,
                  background: i <= 1 ? 'var(--vend)' : 'var(--ink-200)',
                  border: i === 1 ? '4px solid var(--vend-soft)' : 'none',
                  boxSizing: 'content-box',
                }}/>
                <div style={{ fontSize: 9.5, fontWeight: 600, color: i <= 1 ? 'var(--vend-dark)' : 'var(--ink-400)', textAlign: 'center' }}>{s}</div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ padding: 16 }}>
          <div className="t-label mb-8">CUSTOMER</div>
          <div className="card row gap-10">
            <Avatar initials="S" bg="var(--cust-soft)" color="var(--cust-dark)" size={40}/>
            <div className="grow">
              <div style={{ fontWeight: 700, fontSize: 14 }}>Shiva R.</div>
              <div className="t-meta">204, Lakshmi Residency · 1.8 km</div>
            </div>
            <div style={{ width: 36, height: 36, borderRadius: 18, background: 'var(--ok-soft)', color: 'var(--ok)', display: 'grid', placeItems: 'center' }}>
              <Icon.Phone size={16}/>
            </div>
          </div>

          <div className="t-label mt-16 mb-8">ITEMS</div>
          <div className="card">
            {[
              { name: '20L Plain Jar', mode: 'Keep · ₹400 deposit', qty: 2, price: 60 },
              { name: 'Mineral 1L × 12', mode: 'Transfer-return', qty: 1, price: 220 },
            ].map((p, i) => (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0',
                borderBottom: i === 0 ? '1px solid var(--ink-100)' : 0,
              }}>
                <div style={{
                  width: 30, height: 30, borderRadius: 8, background: 'var(--vend-soft)',
                  color: 'var(--vend-dark)', display: 'grid', placeItems: 'center', fontWeight: 700,
                }}>×{p.qty}</div>
                <div className="grow">
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{p.name}</div>
                  <div className="t-meta">{p.mode}</div>
                </div>
                <div style={{ fontWeight: 700 }}>₹{p.price * p.qty}</div>
              </div>
            ))}
            <div className="hr"/>
            <div className="row row--sb">
              <span style={{ fontWeight: 700 }}>Order total</span>
              <span style={{ fontWeight: 800, fontSize: 16 }}>₹340</span>
            </div>
          </div>

          <div className="t-label mt-16 mb-8">CONTAINER LEDGER</div>
          <div className="card col gap-8">
            <div className="row row--sb" style={{ fontSize: 13 }}>
              <span>Jars going out</span><span style={{ fontWeight: 700 }}>2</span>
            </div>
            <div className="row row--sb" style={{ fontSize: 13 }}>
              <span>Deposit collected from customer</span><span style={{ fontWeight: 700, color: 'var(--ok)' }}>₹800</span>
            </div>
            <div className="row row--sb" style={{ fontSize: 13 }}>
              <span>Empties expected back</span><span style={{ fontWeight: 700, color: 'var(--ink-700)' }}>2</span>
            </div>
          </div>
        </div>
      </div>

      <div style={{ padding: 16, borderTop: '1px solid var(--ink-100)', background: '#fff', display: 'flex', gap: 10 }}>
        <button className="btn btn--outline" style={{ flex: 1 }}>Print label</button>
        <button className="btn btn--vend" style={{ flex: 2 }}>Mark ready →</button>
      </div>
    </Phone>
  );
}

// V-CAT-01 Catalog
function VendorCatalog() {
  return (
    <Phone>
      <div style={{ padding: '14px 16px 10px', background: '#fff', borderBottom: '1px solid var(--ink-100)' }}>
        <div className="row row--sb">
          <div className="h-lg">Catalog</div>
          <div className="pill pill--vend" style={{ fontSize: 11 }}>8 products</div>
        </div>
        <div className="row gap-8" style={{ marginTop: 12, overflowX: 'auto' }}>
          {['All', '20L Jars', 'Bottles', 'Tankers', 'Draft'].map((t, i) => (
            <div key={t} className="pill" style={{
              fontSize: 12, flexShrink: 0,
              background: i === 0 ? 'var(--vend)' : '#fff',
              color: i === 0 ? '#fff' : 'var(--ink-700)',
              border: i === 0 ? 0 : '1px solid var(--ink-200)',
            }}>{t}</div>
          ))}
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: 12 }}>
        <div className="col gap-10">
          {[
            { name: '20L Plain Jar', price: 60, stock: 'In stock', mode: 'Keep · ₹400 deposit', active: true, count: 'Sold 182 this month' },
            { name: '20L Mineral Jar', price: 95, stock: 'In stock', mode: 'Transfer-return', active: true, count: 'Sold 94 this month' },
            { name: '20L Chilled Jar', price: 80, stock: 'Low (4 left)', mode: 'Keep · ₹400 deposit', active: true, count: 'Sold 31 this month', low: true },
            { name: 'Mineral 1L × 12', price: 220, stock: 'Out of stock', mode: 'Transfer-return', active: false, count: 'Sold 56 this month', oos: true },
          ].map((p, i) => (
            <div key={i} className="card" style={{ padding: 12, opacity: p.active ? 1 : 0.65 }}>
              <div className="row gap-10">
                <div className="placeholder" style={{ width: 60, height: 60, flexShrink: 0, fontSize: 9 }}>PROD</div>
                <div className="grow">
                  <div className="row row--sb">
                    <div style={{ fontWeight: 700, fontSize: 14 }}>{p.name}</div>
                    <div style={{
                      width: 36, height: 20, borderRadius: 10,
                      background: p.active ? 'var(--vend)' : 'var(--ink-200)',
                      position: 'relative', flexShrink: 0,
                    }}>
                      <div style={{
                        position: 'absolute', top: 2, [p.active ? 'right' : 'left']: 2,
                        width: 16, height: 16, borderRadius: 8, background: '#fff',
                      }}/>
                    </div>
                  </div>
                  <div className="t-meta" style={{ marginTop: 2 }}>{p.mode}</div>
                  <div className="row mt-6" style={{ gap: 8 }}>
                    <div style={{ fontWeight: 800, fontSize: 15 }}>₹{p.price}</div>
                    <div className="pill" style={{
                      fontSize: 11,
                      background: p.oos ? 'var(--err-soft)' : p.low ? 'var(--warn-soft)' : 'var(--ok-soft)',
                      color: p.oos ? 'var(--err)' : p.low ? 'var(--warn)' : 'var(--ok)',
                    }}>{p.stock}</div>
                  </div>
                  <div className="t-meta" style={{ marginTop: 6 }}>{p.count}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="fab" style={{ background: 'var(--vend)', bottom: 78 }}><Icon.Plus size={26}/></div>
      <TabBar role="vend" active="cat" items={VEND_TABS}/>
    </Phone>
  );
}

// V-EARN-01 Earnings
function VendorEarnings() {
  return (
    <Phone>
      <div style={{ padding: '14px 16px 10px', background: '#fff', borderBottom: '1px solid var(--ink-100)' }}>
        <div className="h-lg">Earnings</div>
      </div>
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        <div style={{
          background: 'linear-gradient(135deg, var(--vend-dark), var(--vend))',
          borderRadius: 18, padding: 20, color: '#fff', position: 'relative', overflow: 'hidden',
        }}>
          <div style={{ position: 'absolute', right: -30, top: -30, width: 140, height: 140, borderRadius: '50%', background: 'rgba(255,255,255,0.08)' }}/>
          <div style={{ fontSize: 12, opacity: 0.85 }}>AVAILABLE FOR PAYOUT</div>
          <div style={{ fontSize: 36, fontWeight: 800, marginTop: 6 }}>₹18,420</div>
          <div className="row gap-8 mt-12">
            <div className="pill" style={{ background: '#fff', color: 'var(--vend-dark)', fontSize: 12, fontWeight: 700 }}>Withdraw</div>
            <div className="pill" style={{ background: 'rgba(255,255,255,0.2)', color: '#fff', fontSize: 12 }}>Next auto-payout · Fri</div>
          </div>
        </div>

        {/* Range selector */}
        <div className="row mt-20" style={{ gap: 6 }}>
          {['Today', 'Week', 'Month', 'Custom'].map((t, i) => (
            <div key={t} className="pill" style={{
              flex: 1, justifyContent: 'center', fontSize: 12,
              background: i === 1 ? 'var(--ink-900)' : '#fff', color: i === 1 ? '#fff' : 'var(--ink-600)',
              border: i === 1 ? 0 : '1px solid var(--ink-200)',
            }}>{t}</div>
          ))}
        </div>

        {/* Bar chart */}
        <div className="card mt-16" style={{ padding: 14 }}>
          <div className="row row--sb mb-8">
            <div className="t-label">EARNINGS · LAST 7 DAYS</div>
            <div className="t-meta">₹32,840</div>
          </div>
          <svg viewBox="0 0 280 110" width="100%" height="110">
            {[60, 85, 50, 95, 72, 40, 88].map((h, i) => (
              <g key={i}>
                <rect x={12 + i * 38} y={100 - h} width="24" height={h} rx="4" fill={i === 3 ? 'var(--vend)' : 'var(--vend-soft)'}/>
                {i === 3 && <rect x={12 + i * 38} y={100 - h - 6} width="24" height="4" fill="var(--vend-dark)" rx="2"/>}
              </g>
            ))}
            {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => (
              <text key={i} x={24 + i * 38} y="110" textAnchor="middle" fontSize="10" fill="var(--ink-400)" fontWeight="600">{d}</text>
            ))}
          </svg>
        </div>

        {/* Breakdown */}
        <div className="t-label mt-20 mb-8">TODAY'S BREAKDOWN</div>
        <div className="card col gap-8">
          <div className="row row--sb"><span>Gross orders (38)</span><span style={{ fontWeight: 700 }}>₹4,820</span></div>
          <div className="row row--sb"><span>Platform fee (10%)</span><span style={{ color: 'var(--err)' }}>−₹482</span></div>
          <div className="row row--sb"><span>Tips received</span><span style={{ color: 'var(--ok)', fontWeight: 600 }}>+₹65</span></div>
          <div className="row row--sb"><span>Deposits reconciled</span><span style={{ color: 'var(--ok)', fontWeight: 600 }}>+₹1,600</span></div>
          <div className="hr"/>
          <div className="row row--sb" style={{ fontSize: 16, fontWeight: 800 }}><span>Net</span><span>₹6,003</span></div>
        </div>
      </div>
      <TabBar role="vend" active="earn" items={VEND_TABS}/>
    </Phone>
  );
}

// V-SUBS-01 Today's subscriptions
function VendorSubs() {
  return (
    <Phone>
      <TopBar title="Today's subscriptions" subtitle="12 scheduled · 8 delivered"/>

      <div style={{ padding: '12px 16px 0' }}>
        <div className="row gap-8">
          <div className="pill pill--vend" style={{ fontSize: 11 }}>Morning · 6–9 AM</div>
          <div className="pill" style={{ fontSize: 11, border: '1px solid var(--ink-200)', background: '#fff' }}>Evening · 5–8 PM</div>
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: 12 }}>
        {[
          { name: 'Shiva R.', addr: 'Jubilee Hills · 2.1 km', items: '2× 20L Plain', status: 'Delivered', time: '7:12 AM', st: 'ok' },
          { name: 'Priya M.', addr: 'Banjara Hills · 3.4 km', items: '1× 20L Mineral', status: 'Delivered', time: '7:48 AM', st: 'ok' },
          { name: 'Rajesh K.', addr: 'Road No. 12 · 1.6 km', items: '3× 20L Plain', status: 'En-route', time: '8:20 AM', st: 'live' },
          { name: 'Anita D.', addr: 'Krishna Nagar · 4.8 km', items: '1× 20L Plain + 5L × 2', status: 'Skipped', time: '—', st: 'skip' },
          { name: 'Vinay P.', addr: 'MLA Colony · 5.1 km', items: '2× 20L Chilled', status: 'Pending', time: '9:00 AM', st: 'pend' },
        ].map((s, i) => (
          <div key={i} className="card mb-8" style={{ padding: 12 }}>
            <div className="row gap-10">
              <Avatar initials={s.name[0]} bg="var(--vend-soft)" color="var(--vend-dark)" size={40}/>
              <div className="grow">
                <div className="row row--sb">
                  <div style={{ fontWeight: 700, fontSize: 14 }}>{s.name}</div>
                  <div className="pill" style={{ fontSize: 10,
                    background: s.st === 'ok' ? 'var(--ok-soft)' : s.st === 'live' ? 'var(--cust-soft)' : s.st === 'skip' ? 'var(--ink-100)' : 'var(--warn-soft)',
                    color: s.st === 'ok' ? 'var(--ok)' : s.st === 'live' ? 'var(--cust-dark)' : s.st === 'skip' ? 'var(--ink-500)' : 'var(--warn)',
                  }}>{s.status}</div>
                </div>
                <div className="t-meta">{s.addr}</div>
                <div className="row mt-6 row--sb">
                  <div style={{ fontSize: 12, color: 'var(--ink-700)' }}>{s.items}</div>
                  <div className="mono" style={{ fontSize: 11, color: 'var(--ink-500)' }}>{s.time}</div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </Phone>
  );
}

Object.assign(window, {
  VendorDashboard, VendorIncoming, VendorOrderDetail,
  VendorCatalog, VendorEarnings, VendorSubs,
});
