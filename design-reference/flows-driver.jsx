// Neerly — Driver flows

// D-OFF-01 Off-duty
function DriverOffDuty() {
  return (
    <Phone>
      <div style={{ flex: 1, background: '#0E1A24', color: '#fff', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', inset: 0, background: 'radial-gradient(circle at 50% 30%, rgba(240,120,32,0.18), transparent 60%)' }}/>

        <div style={{ position: 'relative', padding: '14px 16px' }}>
          <div className="row">
            <Avatar initials="R" bg="var(--driv)" color="#fff" size={38}/>
            <div className="col grow" style={{ marginLeft: 10, lineHeight: 1.2 }}>
              <div style={{ fontSize: 11, opacity: 0.7 }}>DRIVER ID · NEE-DRV-204</div>
              <div style={{ fontWeight: 700 }}>Ramesh K.</div>
            </div>
            <div className="icon-btn" style={{ color: '#fff' }}><Icon.Bell size={22}/></div>
          </div>
        </div>

        <div style={{ padding: '40px 24px 0', textAlign: 'center' }}>
          <div style={{ fontSize: 13, opacity: 0.65, letterSpacing: 0.5 }}>YOU'RE OFFLINE</div>
          <div className="serif" style={{ fontSize: 38, marginTop: 12, lineHeight: 1.05 }}>
            Good morning,<br/>Ramesh
          </div>
          <div className="t-body" style={{ color: 'rgba(255,255,255,0.65)', marginTop: 10 }}>
            Turn on availability to start receiving deliveries in Jubilee Hills zone.
          </div>
        </div>

        {/* Big toggle */}
        <div style={{ padding: '40px 40px 0', display: 'flex', justifyContent: 'center' }}>
          <div style={{
            width: 160, height: 160, borderRadius: '50%',
            background: 'radial-gradient(circle at 50% 40%, var(--driv), var(--driv-dark))',
            display: 'grid', placeItems: 'center', boxShadow: '0 20px 60px rgba(240,120,32,0.4)',
          }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 30, fontWeight: 800 }}>GO</div>
              <div style={{ fontSize: 10, opacity: 0.9, letterSpacing: 0.4 }}>TAP TO GO ONLINE</div>
            </div>
          </div>
        </div>

        {/* Yesterday card */}
        <div style={{ position: 'absolute', bottom: 100, left: 16, right: 16 }}>
          <div style={{ background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 18, padding: 14, backdropFilter: 'blur(8px)' }}>
            <div className="row row--sb">
              <div style={{ fontSize: 11, opacity: 0.7, letterSpacing: 0.4 }}>YESTERDAY</div>
              <div style={{ fontSize: 11, color: 'var(--driv)', fontWeight: 600 }}>Full report ›</div>
            </div>
            <div className="row" style={{ marginTop: 10, gap: 14 }}>
              <div>
                <div style={{ fontSize: 11, opacity: 0.6 }}>EARNED</div>
                <div style={{ fontSize: 22, fontWeight: 800, marginTop: 2 }}>₹1,240</div>
              </div>
              <div>
                <div style={{ fontSize: 11, opacity: 0.6 }}>DELIVERIES</div>
                <div style={{ fontSize: 22, fontWeight: 800, marginTop: 2 }}>22</div>
              </div>
              <div>
                <div style={{ fontSize: 11, opacity: 0.6 }}>RATING</div>
                <div className="row gap-4" style={{ marginTop: 2 }}>
                  <Icon.Star size={16}/>
                  <span style={{ fontSize: 22, fontWeight: 800 }}>4.9</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <TabBar role="driv" active="today" items={DRIV_TABS}/>
    </Phone>
  );
}

// D-ON-01 On-duty, waiting
function DriverOnDuty() {
  return (
    <Phone>
      {/* Map bg */}
      <div style={{ flex: 1, position: 'relative', background: '#EAF4F3', overflow: 'hidden' }}>
        <svg viewBox="0 0 360 600" width="100%" height="100%" preserveAspectRatio="none">
          <path d="M0 250 Q 90 210 180 260 T 360 240" stroke="#B8D4D0" strokeWidth="22" fill="none"/>
          <path d="M120 0 L 130 600" stroke="#B8D4D0" strokeWidth="14"/>
          <path d="M240 0 L 250 600" stroke="#B8D4D0" strokeWidth="14"/>
          <path d="M0 420 Q 120 380 240 400 T 360 430" stroke="#B8D4D0" strokeWidth="14" fill="none"/>
          <circle cx="180" cy="280" r="50" fill="var(--driv)" opacity="0.08"/>
          <circle cx="180" cy="280" r="30" fill="var(--driv)" opacity="0.12"/>
        </svg>
        {/* Driver pin */}
        <div style={{ position: 'absolute', top: '40%', left: '50%', transform: 'translate(-50%, -50%)' }}>
          <div style={{
            width: 42, height: 42, borderRadius: '50%', background: '#fff',
            display: 'grid', placeItems: 'center', boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
            border: '3px solid var(--driv)',
          }}>
            <Icon.Truck size={20} stroke={2}/>
          </div>
        </div>

        {/* Top status pill */}
        <div style={{ position: 'absolute', top: 12, left: 16, right: 16 }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10,
            background: '#fff', padding: '10px 12px', borderRadius: 14,
            boxShadow: 'var(--shadow-1)',
          }}>
            <span className="live-dot"/>
            <div className="col" style={{ lineHeight: 1.2 }}>
              <div style={{ fontSize: 13, fontWeight: 700 }}>Online · Jubilee Hills</div>
              <div className="t-meta">Waiting for orders · avg 4 min</div>
            </div>
            <div style={{ marginLeft: 'auto' }}>
              <div style={{ width: 36, height: 20, borderRadius: 10, background: 'var(--driv)', position: 'relative' }}>
                <div style={{ position: 'absolute', top: 2, right: 2, width: 16, height: 16, borderRadius: 8, background: '#fff' }}/>
              </div>
            </div>
          </div>
        </div>

        {/* Today strip */}
        <div style={{ position: 'absolute', top: 70, left: 16, right: 16 }}>
          <div className="row" style={{ gap: 8 }}>
            {[
              { k: 'EARNED', v: '₹240' },
              { k: 'DELIVERIES', v: '4' },
              { k: 'HOURS', v: '2h 12m' },
            ].map((s, i) => (
              <div key={i} style={{
                flex: 1, background: '#fff', padding: '8px 10px', borderRadius: 12,
                boxShadow: 'var(--shadow-1)',
              }}>
                <div style={{ fontSize: 9, color: 'var(--ink-500)', letterSpacing: 0.4, fontWeight: 600 }}>{s.k}</div>
                <div style={{ fontWeight: 800, fontSize: 16, marginTop: 2 }}>{s.v}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Tip hint at bottom */}
        <div style={{ position: 'absolute', bottom: 28, left: 16, right: 16 }}>
          <div style={{
            background: 'var(--ink-900)', color: '#fff', padding: '12px 14px', borderRadius: 14,
            display: 'flex', alignItems: 'center', gap: 10, boxShadow: 'var(--shadow-2)',
          }}>
            <Icon.Info size={18}/>
            <div className="grow">
              <div style={{ fontWeight: 700, fontSize: 13 }}>Stay in the zone for faster pings</div>
              <div style={{ fontSize: 11, opacity: 0.75 }}>3 vendors have pending batches nearby</div>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// D-ASSIGN-01 New assignment (bottom sheet over map)
function DriverNewAssign() {
  return (
    <Phone>
      <div style={{ flex: 1, position: 'relative', background: '#EAF4F3' }}>
        <svg viewBox="0 0 360 600" width="100%" height="100%" preserveAspectRatio="none">
          <path d="M0 160 Q 90 120 180 170 T 360 150" stroke="#B8D4D0" strokeWidth="18" fill="none"/>
          <path d="M120 0 L 130 600" stroke="#B8D4D0" strokeWidth="12"/>
          <path d="M80 260 Q 220 220 320 130" stroke="var(--driv)" strokeWidth="3" fill="none" strokeDasharray="6 4"/>
          <circle cx="80" cy="260" r="9" fill="var(--driv)"/>
          <circle cx="320" cy="130" r="10" fill="var(--driv-dark)"/>
        </svg>

        {/* Assignment sheet */}
        <div style={{
          position: 'absolute', left: 0, right: 0, bottom: 0, padding: 16,
        }}>
          <div style={{
            background: '#fff', borderRadius: 22, padding: 18, boxShadow: 'var(--shadow-3)',
            border: '2px solid var(--driv)',
          }}>
            <div className="row row--sb">
              <div className="row gap-8">
                <span className="live-dot" style={{ background: 'var(--driv)' }}/>
                <div className="t-label" style={{ color: 'var(--driv)' }}>NEW DELIVERY</div>
              </div>
              <div className="mono" style={{ fontWeight: 700, fontSize: 18, color: 'var(--driv)' }}>0:18</div>
            </div>

            <div className="row mt-12" style={{ alignItems: 'center', gap: 14 }}>
              <div className="col" style={{ textAlign: 'right' }}>
                <div className="t-meta">PICKUP</div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>Sri Balaji</div>
                <div className="t-meta">1.2 km</div>
              </div>
              <div className="col" style={{ alignItems: 'center' }}>
                <svg width="70" height="20"><line x1="0" y1="10" x2="70" y2="10" stroke="var(--ink-300)" strokeWidth="2" strokeDasharray="4 3"/></svg>
              </div>
              <div className="col">
                <div className="t-meta">DROP</div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>Jubilee Hills</div>
                <div className="t-meta">2.3 km from pickup</div>
              </div>
            </div>

            <div className="hr"/>

            <div className="row row--sb">
              <div className="col">
                <div className="t-meta">YOU'LL EARN</div>
                <div style={{ fontSize: 24, fontWeight: 800, marginTop: 2 }}>₹65</div>
              </div>
              <div className="col" style={{ alignItems: 'flex-end' }}>
                <div className="t-meta">EST. TIME</div>
                <div style={{ fontSize: 14, fontWeight: 700, marginTop: 6 }}>~18 min</div>
              </div>
            </div>

            <div className="row gap-10 mt-16">
              <button className="btn btn--ghost" style={{ flex: 1 }}>Skip</button>
              <button className="btn btn--driv" style={{ flex: 2 }}>Accept →</button>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// D-INPROG-01 In progress (heading to customer)
function DriverInProgress() {
  return (
    <Phone>
      <div style={{ flex: 1, position: 'relative', background: '#EAF4F3', overflow: 'hidden' }}>
        <svg viewBox="0 0 360 500" width="100%" height="100%" preserveAspectRatio="none">
          <path d="M0 140 Q 90 100 180 150 T 360 130" stroke="#B8D4D0" strokeWidth="20" fill="none"/>
          <path d="M120 0 L 130 500" stroke="#B8D4D0" strokeWidth="14"/>
          <path d="M50 400 Q 140 280 200 220 Q 260 160 310 80" stroke="var(--driv)" strokeWidth="4" fill="none"/>
          <circle cx="310" cy="80" r="10" fill="var(--driv-dark)"/>
          <circle cx="50" cy="400" r="10" fill="var(--driv)" opacity="0.3"/>
        </svg>

        {/* Driver puck */}
        <div style={{ position: 'absolute', top: '50%', left: '38%', transform: 'translate(-50%, -50%)' }}>
          <div style={{
            width: 46, height: 46, borderRadius: '50%', background: 'var(--driv)',
            display: 'grid', placeItems: 'center', color: '#fff',
            boxShadow: '0 8px 24px rgba(240,120,32,0.5)', border: '3px solid #fff',
          }}>
            <Icon.Nav size={20}/>
          </div>
        </div>

        {/* Top HUD */}
        <div style={{ position: 'absolute', top: 12, left: 12, right: 12 }}>
          <div className="row" style={{
            background: 'var(--ink-900)', color: '#fff', padding: '10px 12px', borderRadius: 14,
            boxShadow: 'var(--shadow-2)', gap: 12,
          }}>
            <div className="col">
              <div style={{ fontSize: 10, opacity: 0.7, letterSpacing: 0.3 }}>ETA</div>
              <div style={{ fontSize: 20, fontWeight: 800 }}>12 min</div>
            </div>
            <div style={{ width: 1, background: 'rgba(255,255,255,0.15)' }}/>
            <div className="col grow">
              <div style={{ fontSize: 10, opacity: 0.7, letterSpacing: 0.3 }}>HEADING TO</div>
              <div style={{ fontSize: 13, fontWeight: 700 }}>204, Lakshmi Residency</div>
              <div style={{ fontSize: 11, opacity: 0.6 }}>12th Road, Jubilee Hills</div>
            </div>
            <div className="col" style={{ alignItems: 'flex-end', gap: 6 }}>
              <div className="pill" style={{ background: 'rgba(255,255,255,0.2)', color: '#fff', fontSize: 10 }}>2.3 km</div>
            </div>
          </div>
        </div>

        {/* Turn prompt */}
        <div style={{ position: 'absolute', top: 92, left: 12, right: 12 }}>
          <div className="row" style={{
            background: '#fff', padding: '8px 12px', borderRadius: 12, boxShadow: 'var(--shadow-1)', gap: 10,
          }}>
            <div style={{ width: 34, height: 34, borderRadius: 10, background: 'var(--cust)', color: '#fff', display: 'grid', placeItems: 'center' }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 5v8h-5"/><path d="M7 13l-4-4 4-4"/>
              </svg>
            </div>
            <div className="col grow">
              <div style={{ fontWeight: 700, fontSize: 14 }}>Turn left in 400 m</div>
              <div className="t-meta">onto 12th Road</div>
            </div>
          </div>
        </div>

        {/* Order card bottom */}
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, padding: 12 }}>
          <div className="card" style={{ padding: 14, boxShadow: 'var(--shadow-2)' }}>
            <div className="row gap-10">
              <Avatar initials="S" bg="var(--cust-soft)" color="var(--cust-dark)" size={40}/>
              <div className="grow col">
                <div style={{ fontWeight: 700, fontSize: 14 }}>Shiva R.</div>
                <div className="t-meta">2× 20L Plain Jar · COD ₹340</div>
              </div>
              <div className="row gap-6">
                <div style={{ width: 36, height: 36, borderRadius: 18, background: 'var(--ok-soft)', color: 'var(--ok)', display: 'grid', placeItems: 'center' }}><Icon.Phone size={16}/></div>
                <div style={{ width: 36, height: 36, borderRadius: 18, background: 'var(--ink-100)', color: 'var(--ink-700)', display: 'grid', placeItems: 'center' }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8A8.5 8.5 0 018.7 3.9a8.38 8.38 0 013.8-.9A8.5 8.5 0 0121 11.5z"/></svg>
                </div>
              </div>
            </div>
            <div className="hr"/>
            <button className="btn btn--driv btn--full" style={{ fontSize: 15 }}>I've arrived →</button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// D-COMP-01 Complete delivery (photo + OTP + COD)
function DriverComplete() {
  return (
    <Phone>
      <TopBar title="Complete delivery" subtitle="Order #NEE-042"/>
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>

        {/* Step 1 · OTP */}
        <div className="t-label mb-8"><span style={{ color: 'var(--driv)' }}>STEP 1</span> · VERIFY WITH CUSTOMER</div>
        <div className="card" style={{ padding: 16 }}>
          <div className="t-meta">Ask Shiva for the 4-digit OTP shown in their app</div>
          <div className="row mt-12" style={{ gap: 10, justifyContent: 'center' }}>
            {['3', '9', '4', ''].map((d, i) => (
              <div key={i} style={{
                width: 56, height: 62, borderRadius: 12,
                background: i === 3 ? '#fff' : 'var(--ok-soft)',
                border: i === 3 ? '2px solid var(--driv)' : '2px solid var(--ok)',
                display: 'grid', placeItems: 'center',
                fontWeight: 800, fontSize: 28, color: 'var(--ink-900)',
              }}>{d || (i === 3 && <span style={{ width: 2, height: 26, background: 'var(--driv)', display: 'inline-block' }}/>)}</div>
            ))}
          </div>
        </div>

        {/* Step 2 · Photo */}
        <div className="t-label mt-20 mb-8"><span style={{ color: 'var(--driv)' }}>STEP 2</span> · PROOF PHOTO</div>
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div className="placeholder--photo placeholder" style={{ height: 150, borderRadius: 0, borderLeft: 0, borderRight: 0, borderTop: 0, position: 'relative', color: 'var(--ink-500)' }}>
            <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center' }}>
              <div className="col" style={{ alignItems: 'center', gap: 6 }}>
                <Icon.Camera size={28}/>
                <div style={{ fontSize: 11, fontFamily: 'JetBrains Mono' }}>TAP TO CAPTURE</div>
              </div>
            </div>
            <div style={{ position: 'absolute', top: 8, right: 8 }}>
              <div className="pill pill--ok" style={{ fontSize: 10 }}>✓ 1 of 2</div>
            </div>
          </div>
          <div style={{ padding: 12 }}>
            <div className="row gap-8">
              <div className="placeholder--photo placeholder" style={{ width: 60, height: 60 }}>DONE</div>
              <div className="grow">
                <div style={{ fontWeight: 600, fontSize: 13 }}>Photo of jars at door</div>
                <div className="t-meta">Captured 2 min ago · retake</div>
              </div>
            </div>
          </div>
        </div>

        {/* Step 3 · COD */}
        <div className="t-label mt-20 mb-8"><span style={{ color: 'var(--driv)' }}>STEP 3</span> · COLLECT PAYMENT</div>
        <div className="card">
          <div className="row row--sb">
            <span className="t-meta">Amount to collect</span>
            <span style={{ fontSize: 24, fontWeight: 800 }}>₹340</span>
          </div>
          <div className="hr"/>
          <div className="col gap-8">
            {[
              { m: 'Cash', desc: 'Collect exact change', on: true },
              { m: 'UPI QR', desc: 'Show my QR to scan', on: false },
            ].map((p, i) => (
              <div key={i} className={`opt-card ${p.on ? 'is-active' : ''}`} style={{ padding: 12 }}>
                <div className="radio"/>
                <div>
                  <div style={{ fontWeight: 700, fontSize: 14 }}>{p.m}</div>
                  <div className="t-meta">{p.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Empty containers back */}
        <div className="t-label mt-20 mb-8">CONTAINERS RETURNED</div>
        <div className="card row row--sb">
          <div>
            <div style={{ fontWeight: 700, fontSize: 14 }}>2 empty jars collected</div>
            <div className="t-meta">Deposit ₹800 will be refunded to customer</div>
          </div>
          <div className="row gap-4" style={{ background: 'var(--ink-50)', padding: 4, borderRadius: 999 }}>
            <div style={{ width: 26, height: 26, display: 'grid', placeItems: 'center' }}><Icon.Minus size={14}/></div>
            <div style={{ minWidth: 14, textAlign: 'center', fontWeight: 700 }}>2</div>
            <div style={{ width: 26, height: 26, display: 'grid', placeItems: 'center' }}><Icon.Plus size={14}/></div>
          </div>
        </div>
      </div>

      <div style={{ padding: 16, borderTop: '1px solid var(--ink-100)' }}>
        <button className="btn btn--driv btn--full" style={{ fontSize: 15 }}>Complete delivery · Earn ₹65</button>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  DriverOffDuty, DriverOnDuty, DriverNewAssign, DriverInProgress, DriverComplete,
});
