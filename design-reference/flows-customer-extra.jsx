// Customer — extra onboarding screens to complete the first-run flow

// S-CUST-REG-04 Name
function ScreenName() {
  return (
    <Phone>
      <TopBar title=""/>
      <div style={{ padding: '8px 24px', flex: 1, display: 'flex', flexDirection: 'column' }}>
        <div className="h-display">What should we call you?</div>
        <div className="t-body" style={{ marginTop: 8 }}>Only your first name is required.</div>

        <div className="col gap-12" style={{ marginTop: 28 }}>
          <div className="input-group">
            <label>First name</label>
            <div className="field field--focus" style={{ fontWeight: 600 }}>
              Shiva<span style={{ display: 'inline-block', width: 2, height: 20, background: 'var(--cust)', marginLeft: 2, verticalAlign: 'middle' }}/>
            </div>
          </div>
          <div className="input-group">
            <label>Last name <span style={{ color: 'var(--ink-400)', fontWeight: 400 }}>(optional)</span></label>
            <div className="field" style={{ color: 'var(--ink-400)' }}>Reddy</div>
          </div>
          <div className="input-group">
            <label>Email <span style={{ color: 'var(--ink-400)', fontWeight: 400 }}>(optional, for receipts)</span></label>
            <div className="field" style={{ color: 'var(--ink-400)' }}>shiva@example.com</div>
          </div>
          <div className="input-group">
            <label>Referral code <span style={{ color: 'var(--ink-400)', fontWeight: 400 }}>(optional)</span></label>
            <div className="row gap-8">
              <div className="field mono" style={{ flex: 1, textTransform: 'uppercase', fontWeight: 600 }}>NEERLY100</div>
              <div className="pill pill--ok" style={{ fontSize: 12 }}>✓ Applied</div>
            </div>
          </div>
        </div>

        <div style={{ flex: 1 }}/>
        <button className="btn btn--cust btn--full">Continue</button>
      </div>
    </Phone>
  );
}

// S-CUST-REG-06 Location permission
function ScreenLocationPerm() {
  return (
    <Phone>
      <div style={{ flex: 1, padding: '40px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <div style={{ height: 40 }}/>
        <div style={{
          width: 140, height: 140, borderRadius: '50%',
          background: 'var(--cust-softer)',
          display: 'grid', placeItems: 'center',
          marginBottom: 28,
        }}>
          <div style={{
            width: 96, height: 96, borderRadius: '50%',
            background: 'var(--cust-soft)',
            display: 'grid', placeItems: 'center',
            color: 'var(--cust)',
          }}>
            <Icon.Location size={44}/>
          </div>
        </div>
        <div className="h-display" style={{ fontSize: 26 }}>Find vendors near you</div>
        <div className="t-body" style={{ marginTop: 10, maxWidth: 280 }}>
          We use your location to show water suppliers who deliver to your area.
        </div>

        <div className="card" style={{ marginTop: 28, textAlign: 'left', width: '100%' }}>
          <div className="row gap-10" style={{ alignItems: 'flex-start' }}>
            <div style={{ color: 'var(--cust)', marginTop: 1 }}><Icon.Shield size={18}/></div>
            <div className="t-body" style={{ fontSize: 13, lineHeight: 1.5 }}>
              We only use location while you're using Neerly. You can change this later in Settings.
            </div>
          </div>
        </div>

        <div style={{ flex: 1 }}/>
        <button className="btn btn--cust btn--full">Allow access</button>
        <div style={{ marginTop: 14, fontSize: 13, color: 'var(--ink-500)' }}>Enter address manually</div>
      </div>
    </Phone>
  );
}

// System-level Android location permission dialog (over dim)
function ScreenPermDialog() {
  return (
    <Phone>
      {/* Blurred home underneath */}
      <div style={{ position: 'absolute', inset: 0, background: 'var(--ink-100)', filter: 'blur(2px)' }}/>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(14,26,36,0.45)' }}/>

      <div style={{ position: 'relative', flex: 1, display: 'grid', placeItems: 'center', padding: 20 }}>
        <div style={{
          width: '100%', background: '#fff', borderRadius: 28, overflow: 'hidden',
          boxShadow: 'var(--shadow-3)',
        }}>
          <div style={{ padding: '24px 20px 16px', textAlign: 'center' }}>
            <div style={{ width: 48, height: 48, margin: '0 auto 14px', borderRadius: '50%', background: 'var(--cust-soft)', color: 'var(--cust)', display: 'grid', placeItems: 'center' }}>
              <Icon.Location size={24}/>
            </div>
            <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--ink-900)' }}>Allow Neerly to access this device's location?</div>
          </div>
          <div className="col gap-2" style={{ borderTop: '1px solid var(--ink-100)' }}>
            <div style={{ padding: '14px 20px', borderBottom: '1px solid var(--ink-100)', color: 'var(--cust)', fontWeight: 600, textAlign: 'center', fontSize: 15 }}>Precise</div>
            <div style={{ padding: '14px 20px', borderBottom: '1px solid var(--ink-100)', color: 'var(--cust)', fontWeight: 600, textAlign: 'center', fontSize: 15 }}>Approximate</div>
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <div style={{ padding: 14, flex: 1, textAlign: 'center', color: 'var(--ink-700)', fontWeight: 600, fontSize: 14 }}>While using</div>
              <div style={{ padding: 14, flex: 1, textAlign: 'center', color: 'var(--ink-700)', fontWeight: 600, fontSize: 14 }}>Only this time</div>
              <div style={{ padding: 14, flex: 1, textAlign: 'center', color: 'var(--ink-500)', fontWeight: 600, fontSize: 14 }}>Don't allow</div>
            </div>
          </div>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { ScreenName, ScreenLocationPerm, ScreenPermDialog });
