// Neerly — Customer flows
// Screens: Splash, Welcome, Phone, OTP, Name, Location perm, Address (GPS),
// Home, Vendor detail, Product sheet, Cart, Checkout, Order placed, Tracking,
// Subscriptions list + detail, Wallet, Profile, Deposits

const C = {
  // Shortcuts to role accent
  cust: 'var(--cust)', custDark: 'var(--cust-dark)', custSoft: 'var(--cust-soft)',
};

// =============================================================
// S-COMMON-SPLASH
// =============================================================
function ScreenSplash() {
  return (
    <Phone statusBg="var(--cust)" navBg="var(--cust)" dark>
      <div style={{
        flex: 1, background: 'linear-gradient(180deg, var(--cust-dark) 0%, var(--cust) 60%, #2EA3C6 100%)',
        color: '#fff', display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center', padding: '0 32px', position: 'relative',
      }}>
        <div style={{
          width: 92, height: 92, borderRadius: 28, background: 'rgba(255,255,255,0.14)',
          display: 'grid', placeItems: 'center', backdropFilter: 'blur(4px)', marginBottom: 24,
        }}>
          <svg width="54" height="54" viewBox="0 0 24 24" fill="#fff">
            <path d="M12 2s6 7 6 12a6 6 0 01-12 0c0-5 6-12 6-12z"/>
          </svg>
        </div>
        <div className="serif" style={{ fontSize: 48, lineHeight: 1, letterSpacing: '-0.01em' }}>neerly</div>
        <div style={{ marginTop: 10, fontSize: 15, opacity: 0.85, letterSpacing: 0.08 }}>Water, sorted.</div>
        <div style={{ position: 'absolute', bottom: 60, width: 160, height: 3, background: 'rgba(255,255,255,0.25)', borderRadius: 2, overflow: 'hidden' }}>
          <div style={{ width: '62%', height: '100%', background: '#fff' }}/>
        </div>
      </div>
    </Phone>
  );
}

// =============================================================
// S-CUST-REG-01 Welcome
// =============================================================
function ScreenWelcome() {
  return (
    <Phone>
      <div style={{ padding: '8px 16px 0', display: 'flex', justifyContent: 'flex-end' }}>
        <div className="pill" style={{ fontSize: 11 }}>EN · తె</div>
      </div>
      <div style={{ padding: '28px 24px 0', flex: 1, display: 'flex', flexDirection: 'column' }}>
        <LogoMark size={44}/>
        <div className="serif" style={{ fontSize: 40, lineHeight: 1.05, marginTop: 28, color: 'var(--ink-900)' }}>
          Water, <em style={{ color: 'var(--cust)', fontStyle: 'italic' }}>sorted.</em>
        </div>
        <div className="t-body" style={{ marginTop: 12, fontSize: 15, color: 'var(--ink-600)' }}>
          Order clean water from trusted suppliers near you.
        </div>

        {/* Hero placeholder — water can + phone */}
        <div className="placeholder" style={{
          marginTop: 28, height: 240, borderRadius: 18,
          display: 'flex', flexDirection: 'column', gap: 6,
          background: 'linear-gradient(180deg, var(--water-50), #fff)',
          borderColor: 'var(--water-100)'
        }}>
          <div style={{ color: 'var(--cust-dark)', opacity: 0.9, fontSize: 11 }}>ILLUSTRATION · hero</div>
          <div style={{ color: 'var(--ink-400)', fontSize: 10 }}>20L jar + phone</div>
        </div>

        <div style={{ flex: 1 }}/>
        <button className="btn btn--cust btn--full">Get started</button>
        <div style={{ textAlign: 'center', marginTop: 14, fontSize: 13, color: 'var(--ink-600)' }}>
          I sell water → <span style={{ color: 'var(--cust)', fontWeight: 600 }}>Register as vendor</span>
        </div>
        <div style={{ textAlign: 'center', marginTop: 16, fontSize: 11, color: 'var(--ink-400)', lineHeight: 1.5 }}>
          By continuing you agree to our Terms<br/>and Privacy Policy.
        </div>
        <div style={{ height: 8 }}/>
      </div>
    </Phone>
  );
}

// =============================================================
// S-CUST-REG-02 Phone
// =============================================================
function ScreenPhone() {
  return (
    <Phone>
      <TopBar title="" trailing={null}/>
      <div style={{ padding: '8px 24px 24px', flex: 1, display: 'flex', flexDirection: 'column' }}>
        <div className="h-display">Enter your mobile number</div>
        <div className="t-body" style={{ marginTop: 8 }}>We'll send you a one-time password.</div>

        <div style={{ marginTop: 32, display: 'flex', gap: 10 }}>
          <div className="field" style={{ width: 78, background: 'var(--ink-100)', display: 'grid', placeItems: 'center', color: 'var(--ink-700)' }}>+91</div>
          <div className="field field--focus" style={{ flex: 1, fontWeight: 600, letterSpacing: 0.5 }}>
            98765 43210<span style={{
              display: 'inline-block', width: 2, height: 22, background: 'var(--cust)',
              verticalAlign: 'middle', marginLeft: 4, animation: 'none'
            }}/>
          </div>
        </div>

        <div style={{ flex: 1 }}/>
        <button className="btn btn--cust btn--full">Send OTP</button>
        <div style={{ textAlign: 'center', marginTop: 14, fontSize: 12, color: 'var(--ink-500)' }}>
          Trouble signing in? <span style={{ color: 'var(--cust)', fontWeight: 600 }}>Contact support</span>
        </div>
      </div>
      {/* keyboard */}
      <FakeKeyboard numeric/>
    </Phone>
  );
}

// =============================================================
// S-CUST-REG-03 OTP
// =============================================================
function ScreenOTP() {
  return (
    <Phone>
      <TopBar title=""/>
      <div style={{ padding: '8px 24px', flex: 1, display: 'flex', flexDirection: 'column' }}>
        <div className="h-display">Enter OTP</div>
        <div className="t-body" style={{ marginTop: 8 }}>
          Sent to +91 98765 43210 · <span style={{ color: 'var(--cust)', fontWeight: 600 }}>Change</span>
        </div>
        <div className="otp-boxes" style={{ marginTop: 32, justifyContent: 'center' }}>
          <div className="otp-box">4</div>
          <div className="otp-box">8</div>
          <div className="otp-box is-active"><span style={{ width: 2, height: 24, background: 'var(--cust)', display: 'inline-block' }}/></div>
          <div className="otp-box"/>
        </div>
        <div style={{ textAlign: 'center', marginTop: 22, fontSize: 13, color: 'var(--ink-500)' }}>
          Didn't receive OTP? <span style={{ color: 'var(--ink-400)' }}>Resend in 23s</span>
        </div>
        <div style={{ flex: 1 }}/>
      </div>
      <FakeKeyboard numeric/>
    </Phone>
  );
}

// =============================================================
// S-CUST-REG-07A First address with GPS
// =============================================================
function ScreenAddressGPS() {
  return (
    <Phone>
      <TopBar title="Where should we deliver?" trailing={null}/>
      <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {/* Map */}
        <div className="placeholder--map placeholder" style={{
          height: 260, borderRadius: 0, border: 0, position: 'relative', color: 'transparent',
        }}>
          {/* streets lines */}
          <svg viewBox="0 0 360 260" width="100%" height="100%" style={{ position: 'absolute', inset: 0 }}>
            <path d="M0 140 Q 90 130 180 150 T 360 140" stroke="#B8D4D0" strokeWidth="14" fill="none"/>
            <path d="M120 0 L 130 260" stroke="#B8D4D0" strokeWidth="10"/>
            <path d="M240 0 L 250 260" stroke="#B8D4D0" strokeWidth="10"/>
            <circle cx="170" cy="95" r="6" fill="var(--cust)" opacity="0.35"/>
            <circle cx="170" cy="95" r="3" fill="var(--cust)"/>
          </svg>
          {/* pin */}
          <div style={{ position: 'absolute', top: 80, left: '50%', transform: 'translate(-50%,-100%)' }}>
            <svg width="32" height="40" viewBox="0 0 24 30">
              <path d="M12 0c5 0 9 4 9 9 0 8-9 18-9 18S3 17 3 9c0-5 4-9 9-9z" fill="var(--cust)"/>
              <circle cx="12" cy="9" r="3" fill="#fff"/>
            </svg>
          </div>
          <div style={{
            position: 'absolute', top: 16, left: 16, right: 16,
            padding: '8px 12px', background: 'rgba(14,26,36,0.85)', color: '#fff',
            borderRadius: 999, fontSize: 12, textAlign: 'center', fontWeight: 500,
          }}>Drag pin to your exact location</div>
        </div>

        {/* Form */}
        <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
          <div className="t-label" style={{ marginBottom: 8 }}>Address details</div>
          <div className="col gap-10">
            <div className="field field--focus" style={{ padding: '12px 14px' }}>
              <div className="t-meta" style={{ fontSize: 11 }}>Flat / house number</div>
              <div style={{ fontWeight: 600, color: 'var(--ink-900)' }}>204</div>
            </div>
            <div className="field" style={{ padding: '12px 14px' }}>
              <div className="t-meta" style={{ fontSize: 11 }}>Building name</div>
              <div style={{ fontWeight: 600, color: 'var(--ink-900)' }}>Lakshmi Residency</div>
            </div>
            <div className="field" style={{ padding: '12px 14px' }}>
              <div className="t-meta" style={{ fontSize: 11 }}>Street</div>
              <div style={{ fontWeight: 600, color: 'var(--ink-900)' }}>12th Road, Jubilee Hills</div>
            </div>
            <div className="row gap-10">
              <div className="field" style={{ padding: '12px 14px', flex: 1 }}>
                <div className="t-meta" style={{ fontSize: 11 }}>Area</div>
                <div style={{ fontWeight: 600 }}>Jubilee Hills</div>
              </div>
              <div className="field" style={{ padding: '12px 14px', width: 110 }}>
                <div className="t-meta" style={{ fontSize: 11 }}>Pincode</div>
                <div style={{ fontWeight: 600 }}>500033</div>
              </div>
            </div>

            <div className="t-label" style={{ marginTop: 12, marginBottom: 4 }}>Save as</div>
            <div className="row gap-8">
              {['Home', 'Work', 'Other'].map((x, i) => (
                <div key={x} className={`pill ${i === 0 ? 'pill--cust' : ''}`} style={{ padding: '8px 14px', fontSize: 13, borderRadius: 999, border: i === 0 ? '0' : '1px solid var(--ink-200)', background: i === 0 ? 'var(--cust)' : '#fff', color: i === 0 ? '#fff' : 'var(--ink-700)' }}>{x}</div>
              ))}
            </div>
          </div>
        </div>

        <div style={{ padding: 16, borderTop: '1px solid var(--ink-100)' }}>
          <button className="btn btn--cust btn--full">Save address</button>
        </div>
      </div>
    </Phone>
  );
}

// =============================================================
// S-CUST-HOM-01 Home
// =============================================================
function ScreenHome() {
  return (
    <Phone>
      <div style={{ padding: '8px 12px 10px', background: '#fff', borderBottom: '1px solid var(--ink-100)' }}>
        <div className="row">
          <div className="icon-btn" style={{ width: 40, height: 40, display: 'grid', placeItems: 'center' }}><Icon.Menu size={22}/></div>
          <div className="col grow" style={{ lineHeight: 1.1 }}>
            <div className="t-meta" style={{ fontSize: 10, color: 'var(--ink-500)' }}>DELIVER TO</div>
            <div className="row gap-4" style={{ marginTop: 2 }}>
              <div style={{ fontWeight: 700, fontSize: 14, color: 'var(--ink-900)' }}>Home · Jubilee Hills</div>
              <Icon.ChevronDown size={14}/>
            </div>
          </div>
          <div className="icon-btn" style={{ position: 'relative' }}>
            <Icon.Bell size={22}/>
            <span style={{ position: 'absolute', top: 6, right: 8, width: 8, height: 8, borderRadius: 4, background: 'var(--err)' }}/>
          </div>
        </div>
        <div className="field" style={{
          marginTop: 10, display: 'flex', alignItems: 'center', gap: 8,
          padding: '11px 14px', background: 'var(--ink-50)',
        }}>
          <Icon.Search size={18}/>
          <div className="t-body" style={{ color: 'var(--ink-500)' }}>Search vendors or products</div>
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto' }}>
        {/* First-order banner */}
        <div style={{ padding: '12px 16px 0' }}>
          <div className="banner banner--info" style={{ background: 'var(--cust-softer)' }}>
            <Icon.Gift size={18}/>
            <div>
              <div style={{ fontWeight: 700, color: 'var(--cust-dark)' }}>₹100 off your first order</div>
              <div style={{ fontSize: 11, color: 'var(--ink-600)', fontWeight: 500 }}>Use code NEERLY100 at checkout</div>
            </div>
          </div>
        </div>

        {/* Reorder */}
        <div style={{ padding: '20px 16px 8px' }}>
          <div className="h-md">Reorder in one tap</div>
        </div>
        <div style={{ display: 'flex', gap: 12, overflowX: 'auto', padding: '0 16px 4px' }}>
          {[
            { name: 'Sri Balaji Water', item: '2× 20L Plain Jar', price: 120 },
            { name: 'Pure Drops', item: '5L Mineral × 4', price: 240 },
          ].map((v, i) => (
            <div key={i} className="card" style={{ width: 220, flexShrink: 0 }}>
              <div className="placeholder" style={{ height: 84, marginBottom: 10, fontSize: 10 }}>VENDOR · {v.name}</div>
              <div style={{ fontWeight: 700, fontSize: 13 }}>{v.name}</div>
              <div className="t-meta" style={{ marginTop: 2 }}>{v.item}</div>
              <div className="row mt-8 row--sb">
                <div style={{ fontWeight: 700 }}>₹{v.price}</div>
                <div className="pill pill--cust" style={{ fontSize: 11 }}>Reorder</div>
              </div>
            </div>
          ))}
        </div>

        {/* Vendors header */}
        <div style={{ padding: '20px 16px 8px' }}>
          <div className="row row--sb">
            <div className="h-md">Vendors near you</div>
            <div className="t-meta">12 results</div>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', padding: '0 16px 10px' }}>
          {['All', 'Featured', 'Open now', 'Top rated', 'Tanker'].map((x, i) => (
            <div key={x} className="pill" style={{
              fontSize: 12, padding: '6px 14px', flexShrink: 0,
              background: i === 0 ? 'var(--ink-900)' : '#fff',
              color: i === 0 ? '#fff' : 'var(--ink-700)',
              border: i === 0 ? 0 : '1px solid var(--ink-200)'
            }}>{x}</div>
          ))}
        </div>

        {/* Vendor cards */}
        <div style={{ padding: '4px 16px 20px', display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[
            { name: 'Sri Balaji Water', tier: 'Tier-1', rating: 4.7, reviews: 243, eta: '20–30 min', min: 100, dist: '2.1 km', price: '₹60–120', new: false, closed: false },
            { name: 'Pure Drops Suppliers', tier: 'Tier-1', rating: 4.5, reviews: 128, eta: '25–35 min', min: 150, dist: '3.2 km', price: '₹55–140', new: true, closed: false },
            { name: 'AquaFresh Madhapur', tier: 'Tier-2', rating: 4.2, reviews: 61, eta: '— closed', min: 100, dist: '4.0 km', price: '₹70–110', new: false, closed: true },
          ].map((v, i) => (
            <div key={i} className="card" style={{ padding: 12, position: 'relative' }}>
              {v.closed && <div style={{ position: 'absolute', inset: 0, background: 'rgba(255,255,255,0.6)', borderRadius: 14, pointerEvents: 'none' }}/>}
              <div className="row gap-10">
                <div className="placeholder" style={{ width: 60, height: 60, flexShrink: 0, borderRadius: 12, fontSize: 9 }}>LOGO</div>
                <div className="grow col" style={{ gap: 4 }}>
                  <div className="row row--sb">
                    <div style={{ fontWeight: 700, fontSize: 15 }}>{v.name}</div>
                    {v.new && <div className="pill pill--ok" style={{ fontSize: 10 }}>New</div>}
                  </div>
                  <div className="row gap-6">
                    <div className="pill" style={{ padding: '2px 8px', fontSize: 11, background: 'var(--cust-soft)', color: 'var(--cust-dark)' }}>{v.tier}</div>
                    <div className="row gap-4" style={{ color: 'var(--ink-700)', fontSize: 12, fontWeight: 600 }}>
                      <Icon.Star size={13}/> {v.rating} <span style={{ color: 'var(--ink-500)', fontWeight: 500 }}>({v.reviews})</span>
                    </div>
                  </div>
                  <div className="row gap-8" style={{ fontSize: 12, color: 'var(--ink-600)' }}>
                    <span>{v.eta}</span><span style={{ color: 'var(--ink-300)' }}>·</span>
                    <span>{v.dist}</span><span style={{ color: 'var(--ink-300)' }}>·</span>
                    <span>Min ₹{v.min}</span>
                  </div>
                  <div className="row" style={{ marginTop: 2 }}>
                    <div className="pill" style={{ fontSize: 11, background: 'var(--ink-50)' }}>{v.price} / 20L</div>
                  </div>
                </div>
              </div>
              {v.closed && (
                <div style={{ marginTop: 10, padding: '6px 10px', background: 'var(--ink-100)', borderRadius: 8, fontSize: 12, fontWeight: 600 }}>
                  Closed · Opens tomorrow 6 AM
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      <TabBar role="cust" active="home" items={CUST_TABS}/>
    </Phone>
  );
}

// =============================================================
// S-CUST-VEN-01 Vendor Detail
// =============================================================
function ScreenVendorDetail() {
  return (
    <Phone>
      {/* Hero */}
      <div style={{
        height: 180, position: 'relative',
        background: 'linear-gradient(135deg, var(--cust-dark), var(--cust))',
      }}>
        <div style={{ position: 'absolute', top: 12, left: 12, right: 12, display: 'flex', justifyContent: 'space-between' }}>
          <div style={{ width: 36, height: 36, borderRadius: 18, background: 'rgba(0,0,0,0.35)', color: '#fff', display: 'grid', placeItems: 'center' }}><Icon.Back size={20}/></div>
          <div className="row gap-8">
            <div style={{ width: 36, height: 36, borderRadius: 18, background: 'rgba(0,0,0,0.35)', color: '#fff', display: 'grid', placeItems: 'center' }}><Icon.Share size={18}/></div>
            <div style={{ width: 36, height: 36, borderRadius: 18, background: 'rgba(0,0,0,0.35)', color: '#fff', display: 'grid', placeItems: 'center' }}><Icon.Heart size={18}/></div>
          </div>
        </div>
        <div style={{ position: 'absolute', left: 16, right: 16, bottom: 12, color: '#fff' }}>
          <div className="row gap-8">
            <div className="pill" style={{ background: 'rgba(255,255,255,0.2)', color: '#fff', fontSize: 10, letterSpacing: 0.5 }}>TIER-1 VERIFIED</div>
          </div>
          <div style={{ fontSize: 22, fontWeight: 700, marginTop: 8 }}>Sri Balaji Water Suppliers</div>
          <div className="row gap-6" style={{ marginTop: 4, fontSize: 13 }}>
            <Icon.Star size={14}/> 4.7 (243 reviews)
          </div>
        </div>
      </div>

      {/* Info strip */}
      <div className="row" style={{ padding: 14, background: '#fff', borderBottom: '1px solid var(--ink-100)' }}>
        {[
          { l: 'Delivery', v: '20–30 min' },
          { l: 'Min order', v: '₹100' },
          { l: 'Distance', v: '2.1 km' },
        ].map((x, i) => (
          <div key={i} className="col" style={{ flex: 1, alignItems: 'center', borderRight: i < 2 ? '1px solid var(--ink-100)' : 0 }}>
            <div className="t-meta">{x.l}</div>
            <div style={{ fontWeight: 700, fontSize: 14, marginTop: 2 }}>{x.v}</div>
          </div>
        ))}
      </div>

      <div className="row" style={{ padding: '8px 16px', fontSize: 11, color: 'var(--ink-500)', gap: 6 }}>
        <Icon.Shield size={13}/>
        <span>FSSAI 10019011000256 · Open until 9 PM</span>
      </div>

      {/* Tabs */}
      <div className="row" style={{ padding: '0 16px', borderBottom: '1px solid var(--ink-100)' }}>
        {['Products', 'Reviews', 'About'].map((t, i) => (
          <div key={t} style={{
            padding: '10px 4px', marginRight: 20, fontSize: 14, fontWeight: 600,
            color: i === 0 ? 'var(--cust)' : 'var(--ink-500)',
            borderBottom: i === 0 ? '2px solid var(--cust)' : '2px solid transparent',
          }}>{t}</div>
        ))}
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '12px 16px 80px' }}>
        <div className="t-label" style={{ marginBottom: 8 }}>20L JARS · 4</div>
        <div className="col gap-10">
          {[
            { name: 'Plain water', size: '20L', price: 60, mode: 'Keep container (₹400 deposit)' },
            { name: 'Mineral water', size: '20L', price: 95, mode: 'Transfer-return' },
            { name: 'Chilled water', size: '20L', price: 80, mode: 'Keep container (₹400 deposit)' },
          ].map((p, i) => (
            <div key={i} className="card" style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <div className="placeholder" style={{ width: 64, height: 64, flexShrink: 0 }}>PROD</div>
              <div className="grow">
                <div style={{ fontWeight: 700, fontSize: 14 }}>{p.name}</div>
                <div className="t-meta">{p.size} · {p.mode}</div>
                <div style={{ marginTop: 4, fontWeight: 700 }}>₹{p.price}</div>
              </div>
              <button className="btn btn--cust btn--sm" style={{ padding: '6px 14px' }}>Add</button>
            </div>
          ))}
        </div>
        <div className="t-label" style={{ marginTop: 20, marginBottom: 8 }}>BOTTLES · 3</div>
        <div className="card" style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <div className="placeholder" style={{ width: 64, height: 64, flexShrink: 0 }}>PROD</div>
          <div className="grow">
            <div style={{ fontWeight: 700, fontSize: 14 }}>Mineral water · 1L pack</div>
            <div className="t-meta">12 bottles · Transfer-return</div>
            <div style={{ marginTop: 4, fontWeight: 700 }}>₹220</div>
          </div>
          <button className="btn btn--cust btn--sm" style={{ padding: '6px 14px' }}>Add</button>
        </div>
      </div>

      {/* Floating cart bar */}
      <div style={{ position: 'absolute', left: 12, right: 12, bottom: 24 }}>
        <div style={{
          background: 'var(--cust)', color: '#fff', borderRadius: 14, padding: '12px 16px',
          display: 'flex', alignItems: 'center', gap: 12, boxShadow: 'var(--shadow-3)',
        }}>
          <div className="col">
            <div style={{ fontSize: 12, opacity: 0.85 }}>2 items in cart</div>
            <div style={{ fontWeight: 700, fontSize: 15 }}>₹120</div>
          </div>
          <div style={{ flex: 1 }}/>
          <div className="row gap-6" style={{ fontWeight: 700, fontSize: 14 }}>
            View cart <Icon.Chevron size={16}/>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// =============================================================
// S-CUST-CRT-01 Cart
// =============================================================
function ScreenCart() {
  return (
    <Phone>
      <TopBar title="Your cart"/>
      <div style={{ flex: 1, overflow: 'auto' }}>
        <div style={{ padding: '12px 16px' }}>
          <div className="row gap-10" style={{ padding: 10, background: 'var(--ink-50)', borderRadius: 12 }}>
            <div className="placeholder" style={{ width: 36, height: 36, fontSize: 8 }}>LOGO</div>
            <div className="col grow">
              <div style={{ fontWeight: 700, fontSize: 13 }}>Sri Balaji Water</div>
              <div className="t-meta">Jubilee Hills · 2.1 km</div>
            </div>
            <div className="pill pill--cust" style={{ fontSize: 11 }}>Tier-1</div>
          </div>
        </div>

        <div className="hr" style={{ margin: 0 }}/>
        {[
          { name: 'Plain 20L Jar', mode: 'Keep container', price: 60, qty: 2 },
          { name: 'Mineral 1L × 12', mode: 'Transfer-return', price: 220, qty: 1 },
        ].map((p, i) => (
          <div key={i} style={{ padding: '14px 16px', display: 'flex', gap: 12, alignItems: 'center', borderBottom: '1px solid var(--ink-100)' }}>
            <div className="placeholder" style={{ width: 56, height: 56, flexShrink: 0 }}>PROD</div>
            <div className="grow col gap-4">
              <div style={{ fontWeight: 700, fontSize: 14 }}>{p.name}</div>
              <div className="t-meta">{p.mode} · <span style={{ color: 'var(--cust)', fontWeight: 600 }}>Edit</span></div>
              <div style={{ fontWeight: 700, fontSize: 14, marginTop: 2 }}>₹{p.price * p.qty}</div>
            </div>
            <div style={{
              display: 'flex', alignItems: 'center', gap: 4,
              background: 'var(--ink-50)', borderRadius: 999, padding: 4,
            }}>
              <div style={{ width: 28, height: 28, display: 'grid', placeItems: 'center', color: 'var(--cust)' }}><Icon.Minus size={16}/></div>
              <div style={{ minWidth: 16, textAlign: 'center', fontWeight: 700 }}>{p.qty}</div>
              <div style={{ width: 28, height: 28, display: 'grid', placeItems: 'center', color: 'var(--cust)' }}><Icon.Plus size={16}/></div>
            </div>
          </div>
        ))}

        {/* Promo */}
        <div style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 10, borderBottom: '1px solid var(--ink-100)' }}>
          <div style={{ width: 36, height: 36, background: 'var(--ok-soft)', color: 'var(--ok)', display: 'grid', placeItems: 'center', borderRadius: 10 }}><Icon.Gift size={18}/></div>
          <div className="grow">
            <div style={{ fontWeight: 700, fontSize: 13 }}>NEERLY100 applied</div>
            <div className="t-meta">₹100 off your first order</div>
          </div>
          <div style={{ color: 'var(--ink-400)' }}><Icon.Close size={16}/></div>
        </div>

        {/* Bill */}
        <div style={{ padding: 16 }}>
          <div className="t-label" style={{ marginBottom: 8 }}>BILL DETAILS</div>
          <div className="col gap-8" style={{ fontSize: 13, color: 'var(--ink-700)' }}>
            <div className="row row--sb"><span>Subtotal</span><span>₹340</span></div>
            <div className="row row--sb"><span>Delivery fee</span><span>₹25</span></div>
            <div className="row row--sb"><span>Deposit (refundable)</span><span>₹800</span></div>
            <div className="row row--sb" style={{ color: 'var(--ok)' }}><span>Discount (NEERLY100)</span><span>−₹100</span></div>
            <div className="hr" style={{ margin: '6px 0' }}/>
            <div className="row row--sb" style={{ fontSize: 17, fontWeight: 700, color: 'var(--ink-900)' }}>
              <span>Total</span><span>₹1,065</span>
            </div>
          </div>
        </div>

        <div className="hr" style={{ margin: 0 }}/>
        <div style={{ padding: 16 }}>
          <div className="t-label" style={{ marginBottom: 8 }}>DELIVER TO</div>
          <div className="card row gap-10" style={{ padding: 12, alignItems: 'flex-start' }}>
            <div style={{ color: 'var(--cust)' }}><Icon.Location size={18}/></div>
            <div className="grow">
              <div style={{ fontWeight: 700, fontSize: 14 }}>Home · Jubilee Hills</div>
              <div className="t-meta">204, Lakshmi Residency, 12th Road, 500033</div>
            </div>
            <div style={{ color: 'var(--cust)', fontWeight: 600, fontSize: 13 }}>Change</div>
          </div>
          <div className="t-label" style={{ marginTop: 14, marginBottom: 8 }}>DELIVERY SLOT</div>
          <div className="opt-card is-active">
            <div className="radio"/>
            <div>
              <div style={{ fontWeight: 700, fontSize: 14 }}>As soon as possible</div>
              <div className="t-meta">Arrives in 20–30 min</div>
            </div>
          </div>
        </div>
      </div>

      <div style={{ padding: 16, borderTop: '1px solid var(--ink-100)' }}>
        <button className="btn btn--cust btn--full" style={{ justifyContent: 'space-between', padding: '14px 18px' }}>
          <span>Proceed to pay</span><span>₹1,065 →</span>
        </button>
      </div>
    </Phone>
  );
}

// =============================================================
// S-CUST-CHK-03 Order placed
// =============================================================
function ScreenOrderPlaced() {
  return (
    <Phone>
      <div style={{ flex: 1, background: 'linear-gradient(180deg, var(--cust-softer), #fff)', padding: '40px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <div style={{
          width: 92, height: 92, borderRadius: '50%',
          background: 'var(--ok)', color: '#fff',
          display: 'grid', placeItems: 'center',
          boxShadow: '0 0 0 12px var(--ok-soft)',
          marginTop: 40,
        }}>
          <Icon.Check size={44} stroke={3}/>
        </div>
        <div className="h-display" style={{ marginTop: 24, fontSize: 30 }}>Order placed!</div>
        <div className="t-body" style={{ marginTop: 8 }}>
          Finding nearest vendor…
        </div>
        <div className="card" style={{ marginTop: 28, width: '100%', textAlign: 'left' }}>
          <div className="row row--sb">
            <div className="t-meta">ORDER NUMBER</div>
            <div className="mono" style={{ fontSize: 12, color: 'var(--ink-700)' }}>NEE-2026-042200001</div>
          </div>
          <div className="hr"/>
          <div className="row gap-8" style={{ fontSize: 13 }}>
            <Icon.Clock size={16}/> Estimated delivery <strong style={{ marginLeft: 4 }}>20–30 min</strong>
          </div>
          <div className="row gap-8" style={{ fontSize: 13, marginTop: 8 }}>
            <Icon.Location size={16}/> Home · Jubilee Hills
          </div>
          <div className="row gap-8" style={{ fontSize: 13, marginTop: 8 }}>
            <Icon.Wallet size={16}/> Paid ₹1,065 via UPI
          </div>
        </div>
        <div style={{ flex: 1 }}/>
        <button className="btn btn--cust btn--full" style={{ marginTop: 28 }}>Track order</button>
        <div style={{ marginTop: 10, fontSize: 13, color: 'var(--ink-500)' }}>Back to home</div>
      </div>
    </Phone>
  );
}

// =============================================================
// S-CUST-TRK-01 Live tracking
// =============================================================
function ScreenTracking() {
  return (
    <Phone>
      <div style={{ padding: '8px 12px', display: 'flex', alignItems: 'center', gap: 8, background: '#fff', borderBottom: '1px solid var(--ink-100)' }}>
        <div className="icon-btn"><Icon.Back size={20}/></div>
        <div className="col grow" style={{ lineHeight: 1.15 }}>
          <div style={{ fontWeight: 700, fontSize: 14 }}>Order #NEE-2026-001</div>
          <div className="t-meta">Dispatched · arriving in ~12 min</div>
        </div>
        <div className="live-dot"/>
      </div>

      {/* Stepper */}
      <div style={{ padding: '14px 12px 0', background: '#fff' }}>
        <div className="row" style={{ gap: 0, justifyContent: 'space-between' }}>
          {['Placed', 'Accepted', 'Preparing', 'Dispatched', 'Delivered'].map((s, i) => (
            <div key={s} className="col" style={{ alignItems: 'center', flex: 1, gap: 4 }}>
              <div style={{
                width: 18, height: 18, borderRadius: 9,
                background: i <= 3 ? 'var(--cust)' : 'var(--ink-200)',
                border: i === 3 ? '4px solid var(--cust-soft)' : 'none',
                boxSizing: 'content-box',
              }}/>
              <div style={{ fontSize: 10, fontWeight: 600, color: i <= 3 ? 'var(--ink-900)' : 'var(--ink-400)' }}>{s}</div>
            </div>
          ))}
        </div>
        <div style={{ height: 1, background: 'var(--ink-100)', margin: '12px 0 0' }}/>
      </div>

      {/* Map */}
      <div style={{ position: 'relative', height: 220, background: '#EAF4F3', overflow: 'hidden' }}>
        <svg viewBox="0 0 360 220" width="100%" height="100%">
          <path d="M0 120 Q 90 90 180 120 T 360 150" stroke="#B8D4D0" strokeWidth="14" fill="none"/>
          <path d="M120 0 L 130 220" stroke="#B8D4D0" strokeWidth="10"/>
          <path d="M240 0 L 250 220" stroke="#B8D4D0" strokeWidth="10"/>
          <path d="M60 180 Q 140 100 200 110 Q 260 120 300 60" stroke="var(--cust)" strokeWidth="3" fill="none" strokeDasharray="6 4"/>
          <circle cx="300" cy="60" r="7" fill="var(--cust)"/>
          <circle cx="60" cy="180" r="8" fill="var(--cust-dark)"/>
        </svg>
        <div style={{ position: 'absolute', left: 280, top: 32, padding: '4px 10px', background: '#fff', borderRadius: 999, fontSize: 11, fontWeight: 700, boxShadow: 'var(--shadow-1)' }}>
          12 min
        </div>
      </div>

      {/* OTP card */}
      <div style={{ padding: 12 }}>
        <div className="card" style={{ padding: 14, background: 'var(--cust-softer)', border: '1px dashed var(--cust)' }}>
          <div className="t-label" style={{ color: 'var(--cust-dark)' }}>SHARE THIS OTP WITH DRIVER</div>
          <div className="row gap-10" style={{ marginTop: 8 }}>
            {['3', '9', '4', '7'].map((d, i) => (
              <div key={i} style={{ width: 52, height: 56, display: 'grid', placeItems: 'center', background: '#fff', borderRadius: 12, fontWeight: 800, fontSize: 24, color: 'var(--cust-dark)' }}>{d}</div>
            ))}
          </div>
        </div>
      </div>

      {/* Driver */}
      <div style={{ padding: '0 12px 12px' }}>
        <div className="card row gap-10">
          <Avatar initials="R" bg="var(--driv-soft)" color="var(--driv-dark)" size={44}/>
          <div className="grow col" style={{ gap: 2 }}>
            <div style={{ fontWeight: 700, fontSize: 14 }}>Ramesh K.</div>
            <div className="t-meta">Hero Splendor · AP 09 XY 4821</div>
          </div>
          <div style={{ width: 40, height: 40, borderRadius: 20, background: 'var(--ok)', color: '#fff', display: 'grid', placeItems: 'center' }}>
            <Icon.Phone size={18}/>
          </div>
        </div>
      </div>

      <div style={{ flex: 1 }}/>
      <div style={{ padding: 12, borderTop: '1px solid var(--ink-100)', display: 'flex', gap: 10 }}>
        <button className="btn btn--ghost" style={{ flex: 1 }}>Cancel order</button>
        <button className="btn btn--outline" style={{ flex: 1 }}>Contact vendor</button>
      </div>
    </Phone>
  );
}

// =============================================================
// S-CUST-SUB-01 Subscriptions
// =============================================================
function ScreenSubs() {
  return (
    <Phone>
      <div style={{ padding: '16px 16px 8px', background: '#fff', borderBottom: '1px solid var(--ink-100)' }}>
        <div className="h-lg">Your subscriptions</div>
        <div className="t-meta" style={{ marginTop: 4 }}>2 active · next delivery tomorrow 7 AM</div>
      </div>
      <div style={{ flex: 1, overflow: 'auto', padding: 12 }}>
        <div className="card" style={{ padding: 14, marginBottom: 10 }}>
          <div className="row gap-10">
            <div className="placeholder" style={{ width: 44, height: 44, fontSize: 8 }}>LOGO</div>
            <div className="grow">
              <div style={{ fontWeight: 700, fontSize: 14 }}>Sri Balaji Water</div>
              <div className="t-meta">2× 20L Plain Jar</div>
            </div>
            <div className="pill pill--ok" style={{ fontSize: 10 }}>● Active</div>
          </div>
          <div className="hr"/>
          <div className="row" style={{ gap: 10, fontSize: 12, color: 'var(--ink-700)' }}>
            <Icon.Sync size={14}/> Every alternate day · 7–9 AM
          </div>
          <div className="row mt-8" style={{ gap: 10, fontSize: 12, color: 'var(--ink-700)' }}>
            <Icon.Calendar size={14}/> Next: Tomorrow, Apr 24
          </div>
          <div className="row mt-12" style={{ gap: 8 }}>
            <div className="pill" style={{ fontSize: 11, border: '1px solid var(--ink-200)', background: '#fff' }}>Skip tomorrow</div>
            <div className="pill" style={{ fontSize: 11, border: '1px solid var(--ink-200)', background: '#fff' }}>Pause</div>
            <div className="pill" style={{ fontSize: 11, border: '1px solid var(--ink-200)', background: '#fff' }}>Details</div>
          </div>
        </div>

        <div className="card" style={{ padding: 14 }}>
          <div className="row gap-10">
            <div className="placeholder" style={{ width: 44, height: 44, fontSize: 8 }}>LOGO</div>
            <div className="grow">
              <div style={{ fontWeight: 700, fontSize: 14 }}>Pure Drops</div>
              <div className="t-meta">5L Mineral × 4</div>
            </div>
            <div className="pill pill--warn" style={{ fontSize: 10 }}>Paused</div>
          </div>
          <div className="hr"/>
          <div className="row" style={{ gap: 10, fontSize: 12, color: 'var(--ink-700)' }}>
            <Icon.Clock size={14}/> Resumes Apr 29
          </div>
        </div>
      </div>
      {/* FAB */}
      <div className="fab" style={{ background: 'var(--cust)', position: 'absolute', bottom: 84 }}>
        <Icon.Plus size={24}/>
      </div>
      <TabBar role="cust" active="subs" items={CUST_TABS}/>
    </Phone>
  );
}

// =============================================================
// S-CUST-WAL-01 Wallet
// =============================================================
function ScreenWallet() {
  return (
    <Phone>
      <div style={{ padding: '16px 16px 8px', background: '#fff' }}>
        <div className="h-lg">Wallet</div>
      </div>
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        <div style={{
          borderRadius: 18, padding: 20,
          background: 'linear-gradient(135deg, var(--ink-900), var(--cust-dark))',
          color: '#fff', position: 'relative', overflow: 'hidden',
        }}>
          <div style={{ position: 'absolute', right: -20, top: -20, width: 120, height: 120, borderRadius: '50%', background: 'rgba(255,255,255,0.08)' }}/>
          <div style={{ fontSize: 12, opacity: 0.8 }}>AVAILABLE BALANCE</div>
          <div style={{ fontSize: 36, fontWeight: 800, marginTop: 8 }}>₹230<span style={{ opacity: 0.7, fontSize: 24, fontWeight: 600 }}>.00</span></div>
          <div className="row mt-12" style={{ gap: 8 }}>
            <div className="pill" style={{ background: '#fff', color: 'var(--ink-900)', fontSize: 12 }}>+ Add money</div>
            <div className="pill" style={{ background: 'rgba(255,255,255,0.2)', color: '#fff', fontSize: 12 }}>Deposits · ₹2,400</div>
          </div>
        </div>

        <div className="row row--sb mt-20 mb-8">
          <div className="t-label">RECENT TRANSACTIONS</div>
          <div style={{ color: 'var(--cust)', fontWeight: 600, fontSize: 12 }}>See all</div>
        </div>

        <div className="col gap-8">
          {[
            { title: 'Order NEE-001 paid', sub: 'Sri Balaji Water · 22 Apr', amt: -845, type: 'debit' },
            { title: 'Referral bonus', sub: 'Code SHIVA2026 · 21 Apr', amt: 75, type: 'credit' },
            { title: 'Added to wallet', sub: 'UPI · 19 Apr', amt: 500, type: 'credit' },
            { title: 'Deposit refund', sub: 'Jar returned · 18 Apr', amt: 400, type: 'credit' },
          ].map((t, i) => (
            <div key={i} className="card row" style={{ padding: 12, gap: 10 }}>
              <div style={{
                width: 34, height: 34, borderRadius: 10, display: 'grid', placeItems: 'center',
                background: t.type === 'credit' ? 'var(--ok-soft)' : 'var(--ink-100)',
                color: t.type === 'credit' ? 'var(--ok)' : 'var(--ink-700)',
              }}>
                <Icon.Wallet size={16}/>
              </div>
              <div className="grow">
                <div style={{ fontWeight: 600, fontSize: 13 }}>{t.title}</div>
                <div className="t-meta">{t.sub}</div>
              </div>
              <div style={{ fontWeight: 700, fontSize: 14, color: t.amt > 0 ? 'var(--ok)' : 'var(--ink-900)' }}>
                {t.amt > 0 ? '+' : '−'}₹{Math.abs(t.amt)}
              </div>
            </div>
          ))}
        </div>
      </div>
      <TabBar role="cust" active="wallet" items={CUST_TABS}/>
    </Phone>
  );
}

// =============================================================
// S-CUST-RET-01 Deposits
// =============================================================
function ScreenDeposits() {
  return (
    <Phone>
      <TopBar title="Your deposits"/>
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        <div className="card" style={{ background: 'var(--cust-softer)', border: '1px solid var(--cust-soft)', padding: 16 }}>
          <div className="t-label" style={{ color: 'var(--cust-dark)' }}>HELD IN DEPOSITS</div>
          <div style={{ fontSize: 30, fontWeight: 800, color: 'var(--cust-dark)', marginTop: 4 }}>₹2,400</div>
          <div className="t-meta" style={{ color: 'var(--cust-dark)', opacity: 0.8 }}>3 containers to return</div>
        </div>

        <div className="t-label mt-20 mb-8">ACTIVE</div>
        <div className="col gap-10">
          <div className="card">
            <div className="row gap-10">
              <div className="placeholder" style={{ width: 56, height: 56 }}>JAR</div>
              <div className="grow">
                <div style={{ fontWeight: 700, fontSize: 14 }}>20L Plain Jar</div>
                <div className="t-meta">Sri Balaji · Order #NEE-001</div>
                <div className="pill pill--ok mt-4" style={{ fontSize: 10 }}>● Return in 2 days</div>
              </div>
              <div style={{ fontWeight: 700 }}>₹400</div>
            </div>
            <div className="hr"/>
            <button className="btn btn--cust btn--sm btn--full">Schedule return</button>
          </div>

          <div className="card">
            <div className="row gap-10">
              <div className="placeholder" style={{ width: 56, height: 56 }}>JAR</div>
              <div className="grow">
                <div style={{ fontWeight: 700, fontSize: 14 }}>20L Plain Jar</div>
                <div className="t-meta">Sri Balaji · Order #NEE-018</div>
                <div className="pill pill--warn mt-4" style={{ fontSize: 10 }}>● Overdue · grace ends tomorrow</div>
              </div>
              <div style={{ fontWeight: 700 }}>₹400</div>
            </div>
            <div className="hr"/>
            <button className="btn btn--err btn--sm btn--full">Return now</button>
          </div>
        </div>
      </div>
    </Phone>
  );
}

// =============================================================
// Keyboard (shared)
// =============================================================
function FakeKeyboard({ numeric = false }) {
  const rows = numeric
    ? [['1','2','3'], ['4','5','6'], ['7','8','9'], ['*','0','⌫']]
    : [['q','w','e','r','t','y','u','i','o','p'], ['a','s','d','f','g','h','j','k','l'], ['z','x','c','v','b','n','m']];
  return (
    <div style={{ background: '#E8EBEE', padding: numeric ? '12px 40px 14px' : '10px 6px 12px' }}>
      {rows.map((r, i) => (
        <div key={i} style={{ display: 'flex', gap: 6, justifyContent: 'center', marginTop: i > 0 ? 8 : 0 }}>
          {r.map(k => (
            <div key={k} style={{
              flex: 1, height: numeric ? 42 : 38, borderRadius: 8,
              background: '#fff', display: 'grid', placeItems: 'center',
              fontSize: numeric ? 20 : 15, fontWeight: 500, color: 'var(--ink-900)',
              boxShadow: '0 1px 0 rgba(0,0,0,0.1)',
            }}>{k}</div>
          ))}
        </div>
      ))}
      {!numeric && (
        <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
          <div style={{ flex: 1, height: 38, background: '#D5DADF', borderRadius: 8 }}/>
          <div style={{ flex: 5, height: 38, background: '#fff', borderRadius: 8 }}/>
          <div style={{ flex: 1, height: 38, background: 'var(--cust)', borderRadius: 8 }}/>
        </div>
      )}
    </div>
  );
}

Object.assign(window, {
  ScreenSplash, ScreenWelcome, ScreenPhone, ScreenOTP, ScreenAddressGPS,
  ScreenHome, ScreenVendorDetail, ScreenCart, ScreenOrderPlaced,
  ScreenTracking, ScreenSubs, ScreenWallet, ScreenDeposits,
});
