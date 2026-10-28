/* Med Support frontend: vanilla JavaScript, same-origin Spring Boot REST API */
const $=s=>document.querySelector(s), esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const state={token:sessionStorage.getItem('med_token')||'',role:sessionStorage.getItem('med_role')||'GUEST',page:'overview',hospitals:[],requests:[],bookings:[],notifications:[],socket:null};
const pages={overview:['⌂','Overview'],hospitals:['✚','Hospitals'],requests:['♡','Blood Requests'],bookings:['▦','Donations'],profile:['♙','My Profile'],admin:['⚙','Administration'],notifications:['♧','Notifications'],auth:['⇥','Sign in']};
function notify(msg,error=false){$('#notice').textContent=msg;$('#notice').className=error?'error':'';window.scrollTo({top:0,behavior:'smooth'});}
async function api(path,opts={}){const headers={...(opts.body instanceof FormData?{}:{'Content-Type':'application/json'}),...(state.token?{Authorization:'Bearer '+state.token}:{})};const res=await fetch('http://localhost:8080' + path,{...opts,headers:{...headers,...opts.headers}});let data;const txt=await res.text();try{data=JSON.parse(txt)}catch{data=txt}if(!res.ok)throw Error((typeof data==='object'?(data.message||data.error):data)||`Request failed (${res.status})`);return data}
function renderNav(){const allowed=state.token?['overview',...(state.role==='HOSPITAL_STAFF'?[]:['hospitals']),'requests','bookings',...(state.role==='HOSPITAL_STAFF'?[]:['notifications']),'profile',...(state.role==='ADMIN'?['admin']:[])]:['overview','hospitals','auth'];$('#nav').innerHTML=allowed.map(k=>`<button data-nav="${k}" class="${state.page===k?'active':''}">${pages[k][0]} <span>${pages[k][1]}</span></button>`).join('');$('#roleBadge').textContent=state.role.replace('_',' ');$('#logout').style.display=state.token?'block':'none';document.querySelectorAll('[data-nav]').forEach(b=>b.onclick=()=>navigate(b.dataset.nav))}
function navigate(p){state.page=p;$('#notice').textContent='';renderNav();$('#pageTitle').textContent=pages[p][1];({overview,hospitals,requests,bookings,profile,admin,notifications,auth})[p]();}
function panel(title,content,subtitle=''){return `<div class="panel"><div class="panel-head"><div><h2>${title}</h2>${subtitle?`<p class="hint">${subtitle}</p>`:''}</div></div>${content}</div>`}
function field(name,label,type='text',extra=''){return `<div class="field"><label for="${name}">${label}</label><input id="${name}" name="${name}" type="${type}" ${extra}></div>`}
function select(name,label,options){return `<div class="field"><label for="${name}">${label}</label><select id="${name}" name="${name}">${options.map(v=>`<option value="${esc(v)}">${esc(v)}</option>`).join('')}</select></div>`}
function val(id){return $('#'+id)?.value||''}
function statusPill(s){const v=String(s||'Unknown');return `<span class="pill ${/OPEN|ACTIVE|CONFIRMED|COMPLETED/i.test(v)?'green':'red'}">${esc(v)}</span>`}
async function loadHospitals(){try{state.hospitals=await api('/api/hospitals');$('#connection').textContent='● API connected'}catch(e){$('#connection').textContent='● API unavailable';throw e}}
// Get the saved notifications count from the database
async function loadNotificationCount() {
    if (!state.token) return;

    try {
        const notifications = await api('/api/notifications/my');

        const counter = document.querySelector('#notificationCount');

        if (counter) {
            counter.textContent = notifications.length;
        }

    } catch (error) {
        console.error('Unable to load notification count:', error);
    }
}
function overview(){const active=state.hospitals.filter(h=>h.hospitalActive).length;$('#view').innerHTML=`<div class="hero"><div><span class="eyebrow" style="color:#ffe1e5">GIVE HOPE. SAVE LIVES.</span><h2>A little help can make<br>a big difference.</h2><p>Discover blood donation requests, connect with hospitals, and help your community when it matters most.</p></div><button class="button" id="explore">Explore blood requests →</button></div><div class="stats"><div class="stat"><span class="icon">✚</span><p>Partner hospitals</p><strong>${state.hospitals.length}</strong></div><div class="stat"><span class="icon">♡</span><p>Active hospitals</p><strong>${active}</strong></div><div class="stat"><span class="icon">♧</span><p>My notifications</p><strong id="notificationCount">0</strong></div></div>${panel('How Med Support works',`<div class="grid"><div class="item"><span class="pill red">01 · FIND</span><h3>Discover urgent needs</h3><p>Browse hospital blood requests and filter by blood type.</p></div><div class="item"><span class="pill red">02 · HELP</span><h3>Book your donation</h3><p>Choose a donation date and time and track your booking.</p></div></div>`)}<p class="footer-note">Live information is provided by your Spring Boot API.</p>`;$('#explore').onclick = () => navigate('requests');

    loadNotificationCount();
}
async function hospitals() {
    try {
        await loadHospitals();
    } catch (error) {
        notify(error.message, true);
    }

    $('#view').innerHTML = `
        <div class="toolbar">
            <input
                id="hospitalSearch"
                placeholder="Search hospitals by name"
            >
        </div>
        <div id="hospitalList"></div>
    `;

    // Search hospitals through the backend API
    $('#hospitalSearch').oninput = async () => {
        const name = val('hospitalSearch').trim();

        try {
            if (name === '') {
                await loadHospitals();
            } else {
                state.hospitals = await api(
                    '/api/hospitals/search?name=' + encodeURIComponent(name)
                );
            }

            drawHospitals();
        } catch (error) {
            notify(error.message, true);
        }
    };

    drawHospitals();
}
function drawHospitals(){const q=val('hospitalSearch').toLowerCase();const hs=state.hospitals.filter(h=>(h.hospitalName+' '+h.hospitalLocation).toLowerCase().includes(q));$('#hospitalList').innerHTML=hs.length?`<div class="grid">${hs.map(h=>`<div class="item"><div class="split"><span class="pill red">✚ Hospital</span>${statusPill(h.hospitalActive?'ACTIVE':'INACTIVE')}</div><h3>${esc(h.hospitalName)}</h3><p>⌖ ${esc(h.hospitalLocation)}</p><p>${esc(h.hospitalDescription)}</p><div class="meta"><span class="pill">☎ ${esc(h.hospitalPhone)}</span><span class="pill">ID ${h.id}</span></div></div>`).join('')}</div>`:'<div class="empty">No hospitals found.</div>'}
async function requests(){if(!state.token){$('#view').innerHTML=panel('Sign in required','<p class="muted">Sign in to view and manage blood requests.</p><button class="button" id="goLogin">Sign in</button>');$('#goLogin').onclick=()=>navigate('auth');return}$('#view').innerHTML='<div class="panel">Loading blood requests…</div>';try{const path=state.role==='DONOR'?'/api/blood-requests/open':state.role==='HOSPITAL_STAFF'?'/api/blood-requests/hospital':'/api/blood-requests?page=0&size=100&sortBy=id&direction=desc';const data=await api(path);state.requests=Array.isArray(data)?data:(data.content||[]);drawRequests()}catch(e){$('#view').innerHTML=panel('Blood requests','<p class="muted">Unable to load requests.</p>');notify(e.message,true)}}
function drawRequests(){const donor=state.role==='DONOR',staff=state.role==='HOSPITAL_STAFF';$('#view').innerHTML=`${staff?panel('Create a blood request',`<form id="requestForm"><div class="form-grid">${select('bloodType','Required blood type',['A+','A-','B+','B-','AB+','AB-','O+','O-'])}${field('units','Units needed','number','min="1" required')}${select('urgency','Urgency',['LOW','MEDIUM','HIGH','CRITICAL'])}${field('neededDate','Needed by','date','required')}</div><div class="field"><label>Description</label><textarea id="description" required></textarea></div><button class="button">Create request</button></form>`):''}<div class="toolbar">${donor?`<select id="bloodFilter"><option value="">All blood types</option>${['A+','A-','B+','B-','AB+','AB-','O+','O-'].map(t=>`<option>${t}</option>`).join('')}</select>`:''}<select id="sortFilter"><option value="new">Newest first</option><option value="old">Oldest first</option></select></div><div id="requestList"></div>`;if(donor)$('#bloodFilter').onchange=async()=>{try{const t=val('bloodFilter');const data=await api(t?'/api/blood-requests/open/blood-type/'+encodeURIComponent(t):'/api/blood-requests/open');state.requests=data;drawRequestCards()}catch(e){notify(e.message,true)}};$('#sortFilter').onchange=drawRequestCards;if(staff)$('#requestForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/blood-requests',{method:'POST',body:JSON.stringify({requiredBloodType:val('bloodType'),requiredBloodUnits:Number(val('units')),bloodRequestUrgency:val('urgency'),bloodNeededDate:val('neededDate'),bloodRequestDescription:val('description')})});notify('Blood request created.');requests()}catch(err){notify(err.message,true)}};drawRequestCards()}
function drawRequestCards(){let list=[...state.requests].sort((a,b)=>val('sortFilter')==='old'?a.id-b.id:b.id-a.id);$('#requestList').innerHTML=list.length?`<div class="grid">${list.map(r=>`<div class="item"><div class="split"><span class="pill red">${esc(r.requiredBloodType)}</span>${statusPill(r.bloodRequestStatus)}</div><h3>${esc(r.hospital?.hospitalName||'Blood donation request')}</h3><p>${esc(r.bloodRequestDescription||'Blood donation needed')}</p><div class="meta"><span class="pill">${esc(r.requiredBloodUnits)} units</span><span class="pill">${esc(r.bloodRequestUrgency)}</span><span class="pill">By ${esc(r.bloodNeededDate)}</span></div><div class="actions">${state.role==='DONOR'?`<button class="button" data-book="${r.id}">Book donation</button>`:''}${state.role==='HOSPITAL_STAFF'?`<select data-request-status="${r.id}" style="width:150px"><option value="">Update status</option>${['OPEN','FULFILLED','CANCELLED','CLOSED'].map(s=>`<option>${s}</option>`).join('')}</select>`:''}</div></div>`).join('')}</div>`:'<div class="empty">No blood requests found.</div>';document.querySelectorAll('[data-book]').forEach(b=>b.onclick=()=>bookingForm(b.dataset.book));document.querySelectorAll('[data-request-status]').forEach(s=>s.onchange=async()=>{if(!s.value)return;if(!confirm(`Change request #${s.dataset.requestStatus} to ${s.value}?`))return;try{await api(`/api/blood-requests/${s.dataset.requestStatus}/status?newStatus=${encodeURIComponent(s.value)}`,{method:'PUT'});notify('Status updated.');requests()}catch(e){notify(e.message,true)}})}
function bookingForm(id){$('#view').innerHTML=panel(`Book donation for request #${esc(id)}`,`<form id="bookForm"><div class="form-grid">${field('donationDate','Donation date','date','required')}${field('donationTime','Donation time','time','required')}</div><div class="field"><label>Notes (optional)</label><textarea id="bookingNotes"></textarea></div><div class="actions"><button class="button">Confirm booking</button><button type="button" class="button secondary" id="backRequests">Back</button></div></form>`);$('#backRequests').onclick=requests;$('#bookForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/donation-bookings',{method:'POST',body:JSON.stringify({bloodRequestId:Number(id),donationBookingDate:val('donationDate'),donationBookingTime:val('donationTime')+':00',notesAboutTheDonationBooking:val('bookingNotes')})});notify('Donation booking submitted.');navigate('bookings')}catch(err){notify(err.message,true)}}}
async function bookings(){if(!state.token){navigate('auth');return}if(state.role==='ADMIN'){$('#view').innerHTML=panel('Bookings','<p class="muted">The current backend does not expose an administrator bookings-list endpoint.</p>');return}if(state.role==='HOSPITAL_STAFF'){$('#view').innerHTML=panel('Find bookings for a blood request',`<form id="findBookings" class="toolbar">${field('bookingRequestId','Blood request ID','number','min="1" required')}<button class="button">View bookings</button></form><div id="bookingResults"></div>`);$('#findBookings').onsubmit=async e=>{e.preventDefault();try{const d=await api(`/api/donation-bookings/blood-request/${val('bookingRequestId')}`);$('#bookingResults').innerHTML=bookingCards(d,true);wireBookingButtons()}catch(err){notify(err.message,true)}};return}try{const data=await api('/api/donation-bookings/donor');$('#view').innerHTML=panel('My donation bookings',bookingCards(data,false));wireBookingButtons()}catch(e){notify(e.message,true)}}

function bookingCards(data, staff) {
    if (!data.length) {
        return '<div class="empty">No bookings found.</div>';
    }

    return `
        <div class="grid">
            ${data.map(b => {
        const status = b.donationBookingStatus;

        // Completed and cancelled bookings cannot be changed
        const isFinal = ['COMPLETED', 'CANCELLED'].includes(status);

        let actions = '';

        if (!isFinal) {
            if (staff) {
                actions = `
                            <select data-booking-status="${b.id}" style="width:180px">
                                <option value="">Change status</option>
                                ${['CONFIRMED', 'COMPLETED', 'CANCELLED']
                    .map(s => `<option value="${s}">${s}</option>`)
                    .join('')}
                            </select>
                        `;
            } else {
                actions = `
                            <button class="button secondary" data-cancel="${b.id}">
                                Cancel booking
                            </button>
                        `;
            }
        }

        return `
                    <div class="item">
                        <div class="split">
                            <span class="pill">Booking #${esc(b.id)}</span>
                            ${statusPill(status)}
                        </div>

                        <h3>
                            ${esc(b.bloodRequest?.hospital?.hospitalName || 'Donation appointment')}
                        </h3>

                        <p>
                            ${esc(b.donationBookingDate)} · ${esc(b.donationBookingTime)}
                        </p>

                        <p>${esc(b.notesAboutTheDonationBooking || '')}</p>

                        <div class="actions">
                            ${actions}
                        </div>
                    </div>
                `;
    }).join('')}
        </div>
    `;
}

function wireBookingButtons(){document.querySelectorAll('[data-cancel]').forEach(b=>b.onclick=async()=>{if(!confirm('Cancel this booking?'))return;try{await api(`/api/donation-bookings/${b.dataset.cancel}/cancel`,{method:'PUT'});notify('Booking cancelled.');bookings()}catch(e){notify(e.message,true)}});document.querySelectorAll('[data-booking-status]').forEach(s=>s.onchange=async()=>{if(!s.value)return;if(!confirm(`Update booking to ${s.value}?`))return;try{await api(`/api/donation-bookings/${s.dataset.bookingStatus}/status?newStatus=${encodeURIComponent(s.value)}`,{method:'PUT'});notify('Booking status updated.');$('#findBookings').requestSubmit()}catch(e){notify(e.message,true)}})}
function profile(){if(!state.token){navigate('auth');return}$('#view').innerHTML=panel('Update profile',`<form id="profileForm">${field('profileName','Full name','text','required')}${field('profilePhone','Phone number','tel','required')}<button class="button">Save changes</button></form>`)+panel('Change password',`<form id="passwordForm">${field('oldPassword','Current password','password','required')}${field('newPassword','New password','password','required')}<button class="button">Change password</button></form>`)+panel('Upload documents',`<div class="grid"><form id="cprForm"><label>CPR document (PDF, JPEG, PNG; up to 5 MB)</label><input type="file" name="file" accept=".pdf,.jpg,.jpeg,.png" required><button class="button section-space">Upload CPR</button></form><form id="imageForm"><label>Profile photo (JPEG, PNG; up to 5 MB)</label><input type="file" name="file" accept=".jpg,.jpeg,.png" required><button class="button section-space">Upload image</button></form></div>`)+panel('Account settings','<p class="muted">Deactivation will disable your account.</p><button class="button secondary" id="deactivateMe">Deactivate my account</button>');$('#profileForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/users/profile',{method:'PUT',body:JSON.stringify({name:val('profileName'),phoneNumber:val('profilePhone')})});notify('Profile updated.')}catch(err){notify(err.message,true)}};$('#passwordForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/auth/change-password',{method:'PUT',body:JSON.stringify({currentPassword:val('oldPassword'),newPassword:val('newPassword')})});notify('Password updated.')}catch(err){notify(err.message,true)}};for(const [form,path] of [['cprForm','/api/users/profile/cpr'],['imageForm','/api/users/profile/image']])$('#'+form).onsubmit=async e=>{e.preventDefault();try{const file=e.target.querySelector('input[type=file]').files[0];if(file.size>5*1024*1024)throw Error('Maximum file size is 5 MB');const body=new FormData();body.append('file',file);await api(path,{method:'POST',body});notify('File uploaded.')}catch(err){notify(err.message,true)}};$('#deactivateMe').onclick=async()=>{if(!confirm('Deactivate your account? This action affects your ability to sign in.'))return;try{await api('/api/users/profile',{method:'DELETE'});signout();notify('Account deactivated.')}catch(e){notify(e.message,true)}}}
function admin(){if(state.role!=='ADMIN'){navigate('overview');return}$('#view').innerHTML=panel('Create hospital',`<form id="hospitalForm"><div class="form-grid">${field('hName','Hospital name','text','required')}${field('hLocation','Location','text','required')}${field('hPhone','Phone','tel','required')}</div><div class="field"><label>Description</label><textarea id="hDescription"></textarea></div><button class="button">Create hospital</button></form>`)+panel('Manage hospitals','<div id="adminHospitals">Loading…</div>')+panel('Manage users',`<form id="userAdminForm"><div class="form-grid">${field('adminUserId','User ID','number','required')}${select('adminUserAction','Action',['deactivate','reactivate'])}</div><button class="button">Apply to user</button></form>`);$('#hospitalForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/admin/hospitals',{method:'POST',body:JSON.stringify({hospitalName:val('hName'),hospitalLocation:val('hLocation'),hospitalPhone:val('hPhone'),hospitalDescription:val('hDescription'),hospitalActive:true})});notify('Hospital created.');admin()}catch(err){notify(err.message,true)}};$('#userAdminForm').onsubmit=async e=>{e.preventDefault();if(!confirm('Apply this account change?'))return;try{await api(`/api/admin/users/${val('adminUserId')}/${val('adminUserAction')}`,{method:'PUT'});notify('User account updated.')}catch(err){notify(err.message,true)}};loadAdminHospitals()}
async function loadAdminHospitals(){try{const hs=await api('/api/admin/hospitals');$('#adminHospitals').innerHTML=hs.length?`<div class="table-wrap"><table><thead><tr><th>ID</th><th>Name</th><th>Location</th><th>Status</th><th>Actions</th></tr></thead><tbody>${hs.map(h=>`<tr><td>${h.id}</td><td>${esc(h.hospitalName)}</td><td>${esc(h.hospitalLocation)}</td><td>${statusPill(h.hospitalActive?'ACTIVE':'INACTIVE')}</td><td><button class="button secondary" data-h-toggle="${h.id}" data-active="${h.hospitalActive}">${h.hospitalActive?'Deactivate':'Reactivate'}</button> <button class="button outline" data-h-edit="${h.id}">Edit</button></td></tr>`).join('')}</tbody></table></div>`:'<div class="empty">No hospitals.</div>';document.querySelectorAll('[data-h-toggle]').forEach(b=>b.onclick=async()=>{const action=b.dataset.active==='true'?'deactivate':'reactivate';if(!confirm(`${action} hospital #${b.dataset.hToggle}?`))return;try{await api(`/api/admin/hospitals/${b.dataset.hToggle}/${action}`,{method:'PUT'});notify('Hospital updated.');loadAdminHospitals()}catch(e){notify(e.message,true)}});document.querySelectorAll('[data-h-edit]').forEach(b=>b.onclick=()=>{const h=hs.find(h=>h.id===Number(b.dataset.hEdit));$('#adminHospitals').innerHTML=`<form id="editHospital"><h3>Edit hospital #${h.id}</h3>${field('editName','Name','text','required')}${field('editLocation','Location','text','required')}${field('editPhone','Phone','tel','required')}<div class="field"><label>Description</label><textarea id="editDescription"></textarea></div><button class="button">Save changes</button></form>`;$('#editName').value=h.hospitalName;$('#editLocation').value=h.hospitalLocation;$('#editPhone').value=h.hospitalPhone;$('#editDescription').value=h.hospitalDescription||'';$('#editHospital').onsubmit=async e=>{e.preventDefault();try{await api(`/api/admin/hospitals/${h.id}`,{method:'PUT',body:JSON.stringify({...h,hospitalName:val('editName'),hospitalLocation:val('editLocation'),hospitalPhone:val('editPhone'),hospitalDescription:val('editDescription')})});notify('Hospital saved.');loadAdminHospitals()}catch(err){notify(err.message,true)}}})}catch(e){$('#adminHospitals').textContent='Unable to load hospitals.';notify(e.message,true)}}
async function notifications() {

    $('#view').innerHTML = panel(
        'My Notifications',
        '<p class="muted">Loading notifications...</p>'
    );

    try {
        // Get saved notifications from the backend
        const savedNotifications = await api('/api/notifications/my');

        // Display notifications
        $('#view').innerHTML = panel(
            'My Notifications',
            `
            <p class="muted">
                Your donation booking notifications are saved here.
            </p>

            <div id="notificationList">
                ${savedNotifications.length
                ? savedNotifications.map(n => `
                        <div class="item" style="margin-top:10px">
                            <strong>
                                ${esc(n.notificationMessage || 'Booking updated')}
                            </strong>
                            <p>
                                ${esc(n.notificationTime || '')}
                                · Booking #${esc(n.donationBookingId)}
                            </p>
                        </div>
                    `).join('')
                : '<div class="empty">No notifications yet.</div>'
            }
            </div>
            `
        );

    } catch (error) {
        $('#view').innerHTML = panel(
            'My Notifications',
            '<div class="empty">Unable to load notifications.</div>'
        );
        console.error(error);
    }
}
function auth(){if(state.token){navigate('overview');return}$('#view').innerHTML=`<div class="auth-layout"><div class="auth-tabs"><button class="button" id="tabLogin">Sign in</button><button class="button secondary" id="tabRegister">Create account</button><button class="button secondary" id="tabForgot">Forgot password</button></div><div id="authContent"></div></div>`;$('#tabLogin').onclick=showLogin;$('#tabRegister').onclick=showRegister;$('#tabForgot').onclick=showForgot;showLogin()}
function showLogin(){$('#authContent').innerHTML=panel('Welcome back',`<form id="loginForm">${field('email','Email','email','required')}${field('password','Password','password','required')}${select('loginRole','Your role',['DONOR','HOSPITAL_STAFF','ADMIN'])}<p class="hint">Select the role assigned to your account. The server still enforces actual permissions.</p><button class="button" style="width:100%">Sign in</button></form>`);$('#loginForm').onsubmit=async e=>{e.preventDefault();try{const d=await api('/api/auth/login',{method:'POST',body:JSON.stringify({email:val('email'),password:val('password')})});const token=d.token||d.jwtToken||d.accessToken;if(!token)throw Error('Login succeeded but the response has no recognized token field.');state.token=token;state.role=val('loginRole');sessionStorage.setItem('med_token',token);sessionStorage.setItem('med_role',state.role);connectSocket();
    await loadHospitals();
    notify('Signed in successfully.');
    navigate('overview')}catch(err){notify(err.message,true)}}}
function showRegister(){$('#authContent').innerHTML=panel('Join Med Support',`<form id="registerForm">${field('rName','Full name','text','required')}${field('rEmail','Email','email','required')}${field('rPhone','Phone number','tel','required')}${field('rPassword','Password','password','required')}${select('rRole','Account type',['DONOR','HOSPITAL_STAFF'])}<button class="button">Create account</button></form><p class="hint">Verify your email before signing in. Hospital staff may require additional approval.</p>`);$('#registerForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/auth/register',{method:'POST',body:JSON.stringify({name:val('rName'),email:val('rEmail'),phoneNumber:val('rPhone'),password:val('rPassword'),role:val('rRole')})});notify('Registration submitted. Check your email for verification.');showLogin()}catch(err){notify(err.message,true)}}}
function showForgot(){$('#authContent').innerHTML=panel('Recover your account',`<form id="forgotForm">${field('forgotEmail','Email address','email','required')}<button class="button">Send reset link</button></form>`);$('#forgotForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/auth/forgot-password?email='+encodeURIComponent(val('forgotEmail')),{method:'POST'});notify('Password reset requested. Check your email.')}catch(err){notify(err.message,true)}}}
function signout(){state.token='';state.role='GUEST';state.notifications=[];sessionStorage.removeItem('med_token');sessionStorage.removeItem('med_role');if(state.socket){try{state.socket.deactivate()}catch{}state.socket=null}navigate('auth')}
function connectSocket(){if(!state.token||!window.StompJs)return;try{const client=new StompJs.Client({connectHeaders: {
        Authorization: 'Bearer ' + state.token
    },webSocketFactory:()=>new SockJS('/ws'),reconnectDelay:5000,onConnect:()=>{client.subscribe('/user/queue/notifications',msg=>{let data;try{data=JSON.parse(msg.body)}catch{data=msg.body}const message=typeof data==='string'?data:(data&&typeof data==='object'?(data.notificationMessage||data.message):'');loadNotificationCount();if(state.page==='notifications')notifications().catch(console.error);if(typeof message==='string'&&message.trim())notify(message);})}});client.activate();state.socket=client}catch(e){console.warn('WebSocket connection unavailable:',e)}}
$('#logout').onclick=signout;const params=new URLSearchParams(location.search);if(params.has('token')){api('/api/auth/verify-email?token='+encodeURIComponent(params.get('token'))).then(()=>notify('Email verified. You can now sign in.')).catch(e=>notify(e.message,true))}if (state.token) {
    loadHospitals().catch(() => {});
    navigate('overview');
    connectSocket();
} else {
    navigate('auth');
}
