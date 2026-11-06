/* Med Support frontend: vanilla JavaScript, same-origin Spring Boot REST API */
const $=s=>document.querySelector(s), esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const state={token:sessionStorage.getItem('med_token')||'',role:sessionStorage.getItem('med_role')||'GUEST',page:'overview',hospitals:[],requests:[],bookings:[],notifications:[],socket:null,requestTab:'active',slotTab:'active',slots:[]};
const pages={overview:['⌂','Overview'],hospitals:['✚','Hospitals'],requests:['♡','Blood Requests'],slots:['◷','Donation Slots'],bookings:['▦','Donations'],profile:['♙','My Profile'],admin:['⚙','Administration'],notifications:['♧','Notifications'],auth:['⇥','Sign in']};
function notify(msg,error=false){$('#notice').textContent=msg;$('#notice').className=error?'error':'';window.scrollTo({top:0,behavior:'smooth'});}
async function api(path,opts={}){const headers={...(opts.body instanceof FormData?{}:{'Content-Type':'application/json'}),...(state.token?{Authorization:'Bearer '+state.token}:{})};const res=await fetch(path,{...opts,headers:{...headers,...opts.headers}});let data;const txt=await res.text();try{data=JSON.parse(txt)}catch{data=txt}if(!res.ok)throw Error((data&&typeof data==='object'?(data.message||data.error):data)||`Request failed (${res.status})`);return data}
const navIcons={"overview":"M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z","hospitals":"M4 21V5h16v16M9 21v-5h6v5M12 7v6M9 10h6","requests":"M12 3S5 11 5 15a7 7 0 0 0 14 0c0-4-7-12-7-12Z","slots":"M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2ZM7 3v4m10-4v4M3 11h18","bookings":"M8 4H5v17h14V4h-3M8 3h8v4H8ZM8 13l3 3 5-5","profile":"M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2","admin":"M4 6h16M4 12h16M4 18h16M8 3v6m8 0v6M9 15v6","notifications":"M5 17h14l-2-3V9A5 5 0 0 0 7 9v5ZM10 21h4","auth":"M14 4h6v16h-6M3 12h12m-4-4 4 4-4 4"};
function navIcon(key){return `<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${navIcons[key]||navIcons.overview}"/></svg>`}
function renderNav(){const allowed=state.token?['overview',...(state.role==='HOSPITAL_STAFF'?[]:['hospitals']),'requests','slots','bookings',...(state.role==='HOSPITAL_STAFF'?[]:['notifications']),'profile',...(state.role==='ADMIN'?['admin']:[])]:['overview','hospitals','auth'];$('#nav').innerHTML=allowed.map(k=>`<button data-nav="${k}" ${state.page===k?'aria-current="page"':''} class="${state.page===k?'active':''}">${navIcon(k)} <span>${pages[k][1]}</span></button>`).join('');$('#roleBadge').textContent=({DONOR:'Donor',HOSPITAL_STAFF:'Hospital staff',ADMIN:'Administrator',GUEST:'Guest'})[state.role]||state.role;$('#logout').style.display=state.token?'block':'none';document.querySelectorAll('[data-nav]').forEach(b=>b.onclick=()=>navigate(b.dataset.nav))}
function navigate(p){profileVisit++;clearStaffApplications();stopRequestDateWatch();stopSlotExpiryWatch();state.page=p;$('#notice').textContent='';renderNav();$('#pageTitle').textContent=pages[p][1];({overview,hospitals,requests,slots,bookings,profile,admin,notifications,auth})[p]();}
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
function overview(){
    const staff=state.role==='HOSPITAL_STAFF';
    $('#view').innerHTML=`<div class="hero hero-donation"><div class="hero-copy"><span class="eyebrow">EVERY DONATION CONNECTS A COMMUNITY</span><h2>Better coordination.<br>More lives supported.</h2><p>${staff?'Coordinate blood requests and donation appointments in one dedicated hospital workspace.':'Connect with hospital blood requests, reserve your donation appointment and keep your next visit on track.'}</p><button class="button" id="explore">${staff?'Manage blood requests':'Explore blood requests'} <span aria-hidden="true">→</span></button></div><div class="hero-art"><svg class="hero-illustration" viewBox="0 0 420 320" role="img" aria-labelledby="donationArtTitle" xmlns="http://www.w3.org/2000/svg">
<title id="donationArtTitle">A blood donation bag and caring hands connected by a heart</title>
<defs><linearGradient id="donationBag" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#fff"/><stop offset="1" stop-color="#f9e8ed"/></linearGradient></defs>
<circle cx="222" cy="156" r="128" fill="#fff" opacity=".08"/>
<circle cx="222" cy="156" r="104" fill="#fff" opacity=".06"/>
<path d="M95 93h20m-10-10v20M352 225h18m-9-9v18" stroke="#f6c8d5" stroke-width="4" stroke-linecap="round"/>
<circle cx="348" cy="76" r="5" fill="#f6c8d5"/><circle cx="75" cy="218" r="4" fill="#f6c8d5"/>
<path d="M250 68V45c0-12-10-20-22-20s-22 8-22 20v23" fill="none" stroke="#f8d8e2" stroke-width="5"/>
<rect x="154" y="62" width="132" height="171" rx="25" fill="url(#donationBag)"/>
<path d="M163 154c31-15 63 14 114 0v52c0 10-8 18-18 18h-78c-10 0-18-8-18-18Z" fill="#cf4d6b"/>
<path d="M220 93s-27 31-27 47a27 27 0 0 0 54 0c0-16-27-47-27-47Z" fill="#a52e4b"/>
<path d="M220 126v24m-12-12h24" stroke="#fff" stroke-width="6" stroke-linecap="round"/>
<path d="M220 234v18c0 18 17 30 34 30h30c21 0 38-17 38-38v-26" fill="none" stroke="#f8d8e2" stroke-width="6" stroke-linecap="round"/>
<rect x="311" y="191" width="22" height="30" rx="7" fill="#fff1f5"/>
<path d="m58 247 26-38c5-7 14-9 20-4 6 4 7 12 3 18l-9 15 51-17c10-3 18 1 20 8 2 6-1 12-9 16l-39 20c-15 7-27 7-39 1Z" fill="#f0becd"/>
<path d="m352 279-26-30c-6-7-15-7-20-2-5 5-5 12 0 18l7 9-43-7c-10-2-17 3-18 10-1 7 4 12 12 14l36 10c16 4 29 2 39-7Z" fill="#f8d8e2"/>
<path d="m89 268-24 27-30-24 24-28Z" fill="#fff3f6"/><path d="m343 278 23 27 25-24-22-26Z" fill="#fff3f6"/>
<circle cx="118" cy="146" r="28" fill="#fff3f6"/>
<path d="M118 161c-5-4-17-12-17-21 0-9 12-12 17-4 5-8 17-5 17 4 0 9-12 17-17 21Z" fill="#b93958"/>
<path d="M111 180c0 18 17 26 36 26" fill="none" stroke="#efb5c7" stroke-width="3" stroke-dasharray="5 7" stroke-linecap="round"/>
</svg></div></div><div class="stats"><div class="stat"><span class="icon" aria-hidden="true">✚</span><p>Partner hospitals</p><strong id="hospitalTotal">—</strong><small>Across the care network</small></div><div class="stat"><span class="icon" aria-hidden="true">♡</span><p>Active hospitals</p><strong id="activeTotal">—</strong><small>Supporting donation care</small></div><div class="stat"><span class="icon" aria-hidden="true">◷</span><p>My notifications</p><strong id="notificationCount">${state.token?'—':'0'}</strong><small>Booking and reminder updates</small></div></div>${panel('Your donation workspace',`<div class="grid"><div class="item"><span class="pill red">01 · BLOOD REQUESTS</span><h3>Connect a need with a donor</h3><p>${staff?'Publish blood requirements and track request status with your care team.':'Browse hospital requests and find where your donation can help.'}</p><button class="button secondary" data-quick="requests">View blood requests →</button></div><div class="item"><span class="pill red">02 · APPOINTMENTS</span><h3>A clear schedule for better care</h3><p>${staff?'Set appointment capacity and monitor how many places remain.':'Choose an available slot, review your booking and schedule a reminder.'}</p><button class="button secondary" data-quick="slots">${staff?'Manage donation slots':'Find donation slots'} →</button></div></div>`)}<p class="footer-note">Hospital availability and appointment status are provided by the connected care network.</p>`;
    $('#explore').onclick=()=>navigate('requests');
    document.querySelectorAll('[data-quick]').forEach(b=>b.onclick=()=>navigate(b.dataset.quick));
    loadHospitals().then(()=>{if(state.page!=='overview')return;$('#hospitalTotal').textContent=state.hospitals.length;$('#activeTotal').textContent=state.hospitals.filter(h=>h.hospitalActive).length;}).catch(e=>{if(state.page==='overview')notify('Hospital information is currently unavailable. '+e.message,true);});
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
                aria-label="Search hospitals by name"
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
function recordsTabs(kind,activeLabel,historyLabel,counts){
    const current=state[kind+'Tab'];
    return '<div class="record-tabs" role="group" aria-label="'+(kind==='request'?'Blood request views':'Donation slot views')+'">'+['active','history'].map((tab,index)=>'<button type="button" class="record-tab '+(current===tab?'selected':'')+'" data-'+kind+'-tab="'+tab+'" aria-pressed="'+(current===tab)+'">'+(index?historyLabel:activeLabel)+' <span>'+counts[tab]+'</span></button>').join('')+'</div>';
}
function requestHistoryLabels(r){
    const expired=typeof r.bloodNeededDate==='string' && r.bloodNeededDate<localToday();
    return (expired?'<span class="pill amber">Expired</span>':'')+(r.bloodRequestStatus!=='OPEN'?statusPill(r.bloodRequestStatus):'');
}
function slotHistoryLabels(s){
    const expired=new Date(s.slotDate+'T'+s.endTime)<=new Date();
    const full=Number(s.reservedCount)>=Number(s.capacity);
    return (expired?'<span class="pill amber">Expired</span>':'')+(full?'<span class="pill red">Full</span>':'');
}
async function loadRequestRecords(path){
    const data=await api(path);
    if(state.role!=='ADMIN'||Array.isArray(data))return Array.isArray(data)?data:(data.content||[]);
    // Include all administrator pages so older history is not silently omitted.
    const records=[...(data.content||[])];
    for(let page=1;page<(data.totalPages||1);page++){
        const next=await api(path.replace('page=0','page='+page));
        records.push(...(next.content||[]));
    }
    return records;
}

async function requests(){stopRequestDateWatch();stopSlotExpiryWatch();if(!state.token){$('#view').innerHTML=panel('Sign in required','<p class="muted">Sign in to view and manage blood requests.</p><button class="button" id="goLogin">Sign in</button>');$('#goLogin').onclick=()=>navigate('auth');return}$('#view').innerHTML='<div class="panel">Loading blood requests…</div>';try{const path=state.role==='DONOR'?'/api/blood-requests/open':state.role==='HOSPITAL_STAFF'?'/api/blood-requests/hospital':'/api/blood-requests?page=0&size=100&sortBy=id&direction=desc';state.requests=await loadRequestRecords(path);try{await loadHospitals()}catch{}drawRequests()}catch(e){$('#view').innerHTML=panel('Blood requests','<p class="muted">Unable to load requests.</p>');notify(e.message,true)}}
function drawRequests(){const donor=state.role==='DONOR',staff=state.role==='HOSPITAL_STAFF';$('#view').innerHTML=`${staff?panel('Create a blood request',`<form id="requestForm"><div class="form-grid">${select('bloodType','Required blood type',['A+','A-','B+','B-','AB+','AB-','O+','O-'])}${field('units','Units needed','number','min="1" required')}${select('urgency','Urgency',['LOW','MEDIUM','HIGH','CRITICAL'])}${field('neededDate','Needed by','date','required')}</div><div class="field"><label>Description</label><textarea id="description" required></textarea></div><button class="button">Create request</button></form>`):''}<div class="toolbar">${donor?`<select id="bloodFilter" aria-label="Filter by blood type"><option value="">All blood types</option>${['A+','A-','B+','B-','AB+','AB-','O+','O-'].map(t=>`<option>${t}</option>`).join('')}</select>`:''}<select id="sortFilter" aria-label="Sort blood requests"><option value="new">Newest first</option><option value="old">Oldest first</option></select></div><div id="requestTabs"></div><div id="requestList"></div>`;if(donor)$('#bloodFilter').onchange=async()=>{try{const t=val('bloodFilter');const data=await api(t?'/api/blood-requests/open/blood-type/'+encodeURIComponent(t):'/api/blood-requests/open');state.requests=data;drawRequestCards()}catch(e){notify(e.message,true)}};$('#sortFilter').onchange=drawRequestCards;if(staff)$('#requestForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/blood-requests',{method:'POST',body:JSON.stringify({requiredBloodType:val('bloodType'),requiredBloodUnits:Number(val('units')),bloodRequestUrgency:val('urgency'),bloodNeededDate:val('neededDate'),bloodRequestDescription:val('description')})});notify('Blood request created.');requests()}catch(err){notify(err.message,true)}};drawRequestCards();watchRequestDate($('#requestList'),drawRequestCards)}
function drawRequestCards(){
    const donor=state.role==='DONOR';
    if(!donor){
        const active=state.requests.filter(requestIsAvailable).length;
        $('#requestTabs').innerHTML=recordsTabs('request','Active Requests','Request History',{active,history:state.requests.length-active});
        document.querySelectorAll('[data-request-tab]').forEach(b=>b.onclick=()=>{state.requestTab=b.dataset.requestTab;drawRequestCards();});
    }
    let list=state.requests.filter(r=>donor?requestIsAvailable(r):state.requestTab==='active'?requestIsAvailable(r):!requestIsAvailable(r)).sort((a,b)=>val('sortFilter')==='old'?a.id-b.id:b.id-a.id);$('#requestList').innerHTML=list.length?`<div class="grid">${list.map(r=>`<div class="item"><div class="split"><span class="pill red">${esc(r.requiredBloodType)}</span>${statusPill(r.bloodRequestStatus)}</div>${!donor&&state.requestTab==='history'?`<div class="meta" aria-label="History classification">${requestHistoryLabels(r)}</div>`:''}<h3>${esc(state.hospitals.find(h=>String(h.id)===String(r.hospitalId))?.hospitalName||'Hospital #'+(r.hospitalId??'unavailable'))}</h3><p>${esc(r.bloodRequestDescription||'Blood donation needed')}</p><div class="meta"><span class="pill">${esc(r.requiredBloodUnits)} units</span><span class="pill">${esc(r.bloodRequestUrgency)}</span><span class="pill">By ${esc(r.bloodNeededDate)}</span></div><div class="actions">${state.role==='DONOR'?`<button class="button" data-book="${r.id}">Book donation</button>`:''}${state.role==='HOSPITAL_STAFF'?`<select data-request-status="${r.id}" style="width:150px"><option value="">Update status</option>${['OPEN','CANCELLED','CLOSED'].map(s=>`<option>${s}</option>`).join('')}</select>`:''}</div></div>`).join('')}</div>`:`<div class="empty">${state.role==='DONOR'?'There are no active blood requests matching your selection. Please check back soon for new donation opportunities.':state.requestTab==='history'?'No historical blood requests to display.':'No active blood requests at this time.'}</div>`;document.querySelectorAll('[data-book]').forEach(b=>b.onclick=()=>bookingForm(b.dataset.book));document.querySelectorAll('[data-request-status]').forEach(s=>s.onchange=async()=>{if(!s.value)return;if(!confirm(`Change request #${s.dataset.requestStatus} to ${s.value}?`))return;try{await api(`/api/blood-requests/${s.dataset.requestStatus}/status?newStatus=${encodeURIComponent(s.value)}`,{method:'PUT'});notify('Status updated.');requests()}catch(e){notify(e.message,true)}})}

// BloodRequest API fields: bloodNeededDate (LocalDate), bloodRequestStatus.
function requestIsAvailable(request){
    const date=request?.bloodNeededDate;
    return request?.bloodRequestStatus==='OPEN' && typeof date==='string'
        && /^\d{4}-\d{2}-\d{2}$/.test(date) && date>=localToday();
}
let requestDateTimer=null;
function stopRequestDateWatch(){clearInterval(requestDateTimer);requestDateTimer=null;}
function watchRequestDate(container,refresh){
    stopRequestDateWatch();
    let day=localToday();
    requestDateTimer=setInterval(()=>{
        if(!container.isConnected){stopRequestDateWatch();return;}
        const today=localToday();
        if(today!==day){day=today;refresh();}
    },1000);
}
async function loadBookableRequest(id){
    // Donors cannot use GET /{id}; refresh through their existing OPEN list API.
    const data=await api('/api/blood-requests/open');
    const list=Array.isArray(data)?data:(data.content||[]);
    const request=list.find(r=>String(r.id)===String(id));
    if(!requestIsAvailable(request))throw Error('This blood request is no longer available. Please choose an active request.');
    return request;
}
function showUnavailableRequest(){
    stopRequestDateWatch();stopSlotExpiryWatch();
    $('#view').innerHTML=panel('Blood request unavailable','<div class="empty">This blood request has expired or is no longer open. Please browse active requests to find another donation opportunity.</div><button class="button secondary section-space" id="backActiveRequests">View active blood requests</button>');
    $('#backActiveRequests').onclick=requests;
}

// Local appointment dates match the backend's LocalDate / LocalDateTime contracts.
function localToday(){const d=new Date();return [d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');}
function slotIsAvailable(s,now=new Date()){
    // A timezone-free ISO timestamp is interpreted in the donor's local time.
    const end=new Date(s.slotDate+'T'+s.endTime);
    return s.status==='OPEN' && Number(s.capacity)>Number(s.reservedCount)
        && Number.isFinite(end.getTime()) && end>now;
}
let slotExpiryTimer=null;
function stopSlotExpiryWatch(){clearInterval(slotExpiryTimer);slotExpiryTimer=null;}
function watchSlotExpiry(container,refresh){
    stopSlotExpiryWatch();
    slotExpiryTimer=setInterval(()=>{
        if(!container.isConnected){stopSlotExpiryWatch();return;}
        refresh();
    },1000);
}
async function chooseSlotRequest(slotId,hospitalId){
    stopRequestDateWatch();stopSlotExpiryWatch();
    $('#view').innerHTML=panel('Choose a blood request','<p class="muted">Finding open requests at this hospital…</p>');
    try {
        const data=await api('/api/blood-requests/open');
        state.requests=Array.isArray(data)?data:(data.content||[]);
        $('#view').innerHTML=panel('Choose a blood request','<p class="muted">Select an active hospital request for your donation. You will review your appointment before confirming.</p><div id="slotRequestChoices" class="grid"></div>');
        const container=$('#slotRequestChoices');
        const drawMatches=()=>{
            const matches=state.requests.filter(r=>requestIsAvailable(r)&&String(r.hospitalId)===String(hospitalId));
            container.innerHTML='';
            if(!matches.length){container.innerHTML='<div class="empty">There are no active blood requests at this hospital right now. Please check back soon for new donation opportunities.</div>';return;}
            for(const request of matches){
                const card=document.createElement('div');card.className='item';
                const title=document.createElement('h3');title.textContent=request.requiredBloodType+' · '+request.requiredBloodUnits+' units';
                const description=document.createElement('p');description.textContent=request.bloodRequestDescription||'Blood donation requested';
                const button=document.createElement('button');button.className='button';button.textContent='Choose request';button.onclick=()=>bookingForm(request.id,slotId);
                card.append(title,description,button);container.append(card);
            }
        };
        drawMatches();watchRequestDate(container,drawMatches);
        const back=document.createElement('button');back.className='button secondary section-space';back.textContent='Back to donation slots';back.onclick=slots;$('#view').append(back);
    }catch(e){$('#view').innerHTML=panel('Blood requests','<div class="empty">Unable to load matching requests.</div>');notify(e.message,true);}
}

async function slots(){
    stopRequestDateWatch();stopSlotExpiryWatch();
    if(!state.token){navigate('auth');return;}
    const staff=state.role==='HOSPITAL_STAFF',manager=staff||state.role==='ADMIN';
    const today=localToday();
    $('#view').innerHTML=panel('Donation appointments','<p class="muted">Loading the appointment schedule…</p>');
    try{
        const all=await api('/api/donation-slots');
        try{await loadHospitals()}catch{}
        state.slots=Array.isArray(all)?all:[];
        $('#view').innerHTML=`<div class="section-intro"><div><span class="eyebrow">APPOINTMENT MANAGEMENT</span><h2>${manager?'Donation appointment management':'Find an open appointment'}</h2><p>${manager?'Review upcoming appointments and retain access to historical donation slots.':'Browse live appointment availability from our partner hospitals.'}</p></div></div>${staff?panel('Create a donation slot',`<form id="slotForm"><div class="form-grid">${field('slotDate','Appointment date','date',`min="${today}" required`)}${field('slotStart','Start time','time','required')}${field('slotEnd','End time','time','required')}${field('slotCapacity','Number of available places','number','min="1" max="1000" value="1" required')}</div><button class="button">Create appointment slot</button></form>`,'Set the appointment date, time and maximum number of donors your team can accommodate.'):''}${panel('Appointment schedule','<div id="slotTabs"></div><div class="slot-grid" id="appointmentSchedule"></div>','Remaining places reflect current reservations. Availability is confirmed when you book.')}`;
        drawSlotCards();
        const schedule=$('#appointmentSchedule');
        let signature=slotViewSignature();
        watchSlotExpiry(schedule,()=>{
            const next=slotViewSignature();
            if(next!==signature){signature=next;drawSlotCards();}
        });
        if(staff){$('#slotForm').onsubmit=async e=>{e.preventDefault();if(val('slotEnd')<=val('slotStart')){notify('End time must be later than start time.',true);return;}try{await api('/api/donation-slots',{method:'POST',body:JSON.stringify({slotDate:val('slotDate'),startTime:val('slotStart')+':00',endTime:val('slotEnd')+':00',capacity:Number(val('slotCapacity'))})});await slots();notify('Donation slot created successfully.')}catch(err){notify(err.message,true)}};}
    }catch(err){
        state.slots=[];
        const tabs=manager?recordsTabs('slot','Upcoming Slots','Slot History',{active:'—',history:'—'}):'';
        const message=state.role==='ADMIN'?'Donation slot records are unavailable for this administrator account. The current server permits slot access to hospital staff and donors only.':err.message;
        $('#view').innerHTML=panel('Donation appointments',tabs+'<div class="empty">'+esc(message)+'</div>');
        document.querySelectorAll('[data-slot-tab]').forEach(b=>b.onclick=()=>{state.slotTab=b.dataset.slotTab;document.querySelectorAll('[data-slot-tab]').forEach(t=>{t.classList.toggle('selected',t===b);t.setAttribute('aria-pressed',String(t===b));});});
    }
}
function slotViewSignature(){return state.slots.map(s=>s.id+':'+slotIsAvailable(s)+':'+slotHistoryLabels(s)).join('|');}
function drawSlotCards(){
    const donor=state.role==='DONOR',staff=state.role==='HOSPITAL_STAFF';
    if(!donor){
        const active=state.slots.filter(s=>slotIsAvailable(s)).length;
        $('#slotTabs').innerHTML=recordsTabs('slot','Upcoming Slots','Slot History',{active,history:state.slots.length-active});
        document.querySelectorAll('[data-slot-tab]').forEach(b=>b.onclick=()=>{state.slotTab=b.dataset.slotTab;drawSlotCards();});
    }
    const filtered=state.slots.filter(s=>donor?slotIsAvailable(s):state.slotTab==='active'?slotIsAvailable(s):!slotIsAvailable(s)).slice().sort((a,b)=>`${a.slotDate} ${a.startTime}`.localeCompare(`${b.slotDate} ${b.startTime}`));
    $('#appointmentSchedule').innerHTML=filtered.length?filtered.map(s=>{
        const remain=Math.max(0,Number(s.capacity)-Number(s.reservedCount));
        return `<article class="slot-card" data-slot-id="${s.id}"><div class="split"><span class="slot-date">${esc(s.slotDate)}</span><span class="slot-status">Status: ${statusPill(s.status)}</span></div>${!donor&&state.slotTab==='history'?`<div class="meta" aria-label="History classification">${slotHistoryLabels(s)}</div>`:''}<h3>${esc(s.startTime)} – ${esc(s.endTime)}</h3><p>${esc(state.hospitals.find(h=>String(h.id)===String(s.hospitalId))?.hospitalName||'Hospital #'+s.hospitalId)}</p><div class="capacity-track"><span style="width:${Math.max(0,Math.min(100,100*Number(s.reservedCount)/Math.max(1,Number(s.capacity))))}%"></span></div><div class="split"><small>${esc(s.reservedCount)} booked / ${esc(s.capacity)} capacity</small><strong class="${slotIsAvailable(s)?'available':'full'}">${remain} left</strong></div>${donor?`<div class="actions"><button class="button" data-slot-book="${s.id}" data-hospital="${s.hospitalId}">Book appointment</button></div>`:''}${staff&&s.status==='OPEN'?`<div class="actions"><button class="button secondary" data-close-slot="${s.id}">Close slot</button></div>`:''}</article>`;
    }).join(''):'<div class="empty">'+(donor?'No appointments are available right now. Please check back soon for new donation opportunities.':state.slotTab==='history'?'No historical donation slots to display.':'No upcoming donation slots are available.')+'</div>';
    document.querySelectorAll('[data-slot-book]').forEach(b=>b.onclick=()=>{
        const slot=state.slots.find(s=>String(s.id)===b.dataset.slotBook);
        if(!slot||!slotIsAvailable(slot)){drawSlotCards();notify('That appointment is no longer available. Please choose another time.',true);return;}
        chooseSlotRequest(b.dataset.slotBook,b.dataset.hospital);
    });
    document.querySelectorAll('[data-close-slot]').forEach(b=>b.onclick=async()=>{
        if(!confirm('Close this donation slot?'))return;
        try{await api('/api/donation-slots/'+b.dataset.closeSlot+'/close',{method:'PATCH'});await slots();notify('Appointment slot closed.');}catch(err){notify(err.message,true);}
    });
}
async function bookingForm(id,selectedSlotId){
    stopRequestDateWatch();stopSlotExpiryWatch();
    $('#view').innerHTML=panel('Choose your donation appointment','<p class="muted">Loading available hospital appointment slots…</p>');
    try {
        const cached=state.requests.find(r=>String(r.id)===String(id));
        if(state.role==='DONOR' && cached && !requestIsAvailable(cached)){showUnavailableRequest();return;}
        const req=state.role==='DONOR'?await loadBookableRequest(id):cached;
        const hospitalId=req?.hospitalId;
        if(!hospitalId) throw Error('Hospital details are unavailable for this request. Refresh blood requests and try again.');
        const available=await api(`/api/donation-slots/available/${hospitalId}`);
        const today=localToday();
        if(state.role==='DONOR' && !requestIsAvailable(req)){showUnavailableRequest();return;}
        const choices=available.filter(s=>slotIsAvailable(s));
        $('#view').innerHTML=panel('Schedule a blood donation',`<div class="context-strip"><span class="context-symbol">♥</span><div><strong>Blood request #${esc(id)}</strong><p>${esc(state.hospitals.find(h=>String(h.id)===String(hospitalId))?.hospitalName||'Hospital #'+hospitalId)} · ${esc(req.requiredBloodType||'')} blood needed</p></div></div>${choices.length?`<form id="bookForm"><div class="field"><label for="slotChoice">Available donation appointments</label><select id="slotChoice" required>${choices.map(s=>`<option value="${s.id}" ${String(s.id)===String(selectedSlotId)?'selected':''}>${esc(s.slotDate)} · ${esc(s.startTime)}–${esc(s.endTime)} · ${s.capacity-s.reservedCount} place(s) remaining</option>`).join('')}</select></div><div class="field"><label for="bookingNotes">Notes (optional)</label><textarea id="bookingNotes" placeholder="Anything the care team should know"></textarea></div><div class="actions"><button class="button">Confirm appointment</button><button type="button" class="button secondary" id="backRequests">Back to requests</button></div></form>`:`<div class="empty">No appointments are available at this hospital right now. Please check back soon for new donation opportunities.</div><button class="button secondary" id="backRequests">Back to requests</button>`}`,'Appointments are assigned by the hospital. Availability may change if multiple donors book at once.');
        $('#backRequests').onclick=requests;
        if(choices.length){
            const form=$('#bookForm');
            watchRequestDate(form,()=>{if(!requestIsAvailable(req))showUnavailableRequest();});
            watchSlotExpiry(form,()=>{
                if(!requestIsAvailable(req)){showUnavailableRequest();return;}
                const select=$('#slotChoice');
                Array.from(select.options).forEach(option=>{
                    const slot=choices.find(s=>String(s.id)===option.value);
                    if(!slot || !slotIsAvailable(slot))option.remove();
                });
                if(!select.options.length){
                    stopSlotExpiryWatch();
                    form.innerHTML='<div class="empty">These appointments have now ended. Please check back soon for new donation opportunities.</div><button type="button" class="button secondary" id="backRequests">Back to requests</button>';
                    $('#backRequests').onclick=requests;
                }
            });
        }
        if(choices.length)$('#bookForm').onsubmit=async e=>{
            e.preventDefault();
            if(state.role==='DONOR' && !requestIsAvailable(req)){showUnavailableRequest();return;}
            const choice=choices.find(s=>String(s.id)===val('slotChoice'));
            if(!choice || !slotIsAvailable(choice)){
                await bookingForm(id);
                notify('That appointment is no longer available. Please choose another time.',true);
                return;
            }
            const btn=e.target.querySelector('button[type="submit"],button.button:not([type])');btn.disabled=true;btn.textContent='Reserving…';
            try {
                if(state.role==='DONOR')await loadBookableRequest(id);
                if(!requestIsAvailable(req)){showUnavailableRequest();return;}
                if(!slotIsAvailable(choice)){await bookingForm(id);notify('That appointment is no longer available. Please choose another time.',true);return;}
                await api('/api/donation-bookings',{method:'POST',body:JSON.stringify({bloodRequestId:Number(id),donationSlotId:choice.id,donationBookingDate:choice.slotDate,donationBookingTime:choice.startTime,notesAboutTheDonationBooking:val('bookingNotes')})});navigate('bookings');notify('Your donation appointment was booked successfully.');}
            catch(err){notify('Booking unavailable. Refresh donation slots to check the latest availability. '+err.message,true);btn.disabled=false;btn.textContent='Confirm appointment';}
        };
    }catch(e){$('#view').innerHTML=panel('Schedule a donation',`<div class="empty">${esc(e.message)}</div><button class="button secondary" id="backRequests">Back to requests</button>`);$('#backRequests').onclick=requests;}
}

async function bookings(){clearStaffApplications();if(!state.token){navigate('auth');return}if(state.role==='ADMIN'){$('#view').innerHTML=panel('Bookings','<p class="muted">The current backend does not expose an administrator bookings-list endpoint.</p>');return}if(state.role==='HOSPITAL_STAFF'){await staffDonations();return}$('#pageTitle').textContent='My Donation Appointments';try{const data=await api('/api/donation-bookings/donor');$('#view').innerHTML='<div class="donor-appointments">'+panel('My Donation Appointments',bookingCards(data,false),'View and manage your booked blood donation appointments. Set reminders, track booking status, or cancel an appointment when needed.')+'</div>';wireBookingButtons()}catch(e){notify(e.message,true)}}

// Hospital-scoped application review. Donor records remain in memory only.
const staffApplications={requests:[],bookings:[],requestId:null,bookingId:null,requestTab:'active',generation:0,selection:0};
function clearStaffApplications(){
    staffApplications.generation++;staffApplications.selection++;
    staffApplications.requests=[];staffApplications.bookings=[];
    staffApplications.requestId=null;staffApplications.bookingId=null;
}
function staffDashboardIsCurrent(generation){return state.token && state.role==='HOSPITAL_STAFF' && generation===staffApplications.generation && !!$('#applicationDashboard');}
function donationPriority(request){return ({CRITICAL:0,HIGH:1,MEDIUM:2,LOW:3})[request.bloodRequestUrgency]??4;}
function bookingTransitions(status){return ({PENDING:['CONFIRMED','CANCELLED'],CONFIRMED:['COMPLETED','CANCELLED']})[status]||[];}
function detailValue(value){
    if(value===null||value===undefined||String(value).trim()==='')return '<span class="unavailable">Not available</span>';
    return esc(value);
}
function detailSection(title,fields){return '<section class="donor-section"><h3>'+title+'</h3><dl class="donor-fields">'+fields.map(([label,value])=>'<div><dt>'+esc(label)+'</dt><dd>'+detailValue(value)+'</dd></div>').join('')+'</dl></section>';}
async function staffDonations(){
    stopRequestDateWatch();stopSlotExpiryWatch();
    if(!state.token||state.role!=='HOSPITAL_STAFF')return;
    clearStaffApplications();const generation=staffApplications.generation;
    $('#view').innerHTML='<div id="applicationDashboard"><div class="section-intro"><span class="eyebrow">HOSPITAL DONATION OPERATIONS</span><h2>Donation applications</h2><p>Review your hospital’s requests, donor information and appointment progress.</p></div><div id="applicationStats"></div><div class="applications-layout"><section class="panel request-worklist"><div class="panel-head"><div><h2>Hospital blood requests</h2><p class="hint">Priority order · includes historical requests</p></div><button type="button" class="button secondary" id="refreshApplications">Refresh</button></div><div id="applicationRequestTabs"></div><div id="applicationRequests"><p class="muted">Loading your hospital’s requests…</p></div></section><div class="application-review"><section class="panel"><div id="applicationRequestContext"></div><div id="applicationBookings" aria-live="polite"><div class="empty">Choose a blood request to review its donation applications.</div></div></section><section class="panel donor-detail-panel" id="applicationDonorDetails" aria-label="Donor application details"><div class="empty">Select an application to review donor details.</div></section></div></div></div>';
    $('#refreshApplications').onclick=staffDonations;
    try{
        const data=await api('/api/blood-requests/hospital');
        if(!staffDashboardIsCurrent(generation))return;
        try{state.slots=await api('/api/donation-slots')}catch{state.slots=[]}
        if(!staffDashboardIsCurrent(generation))return;
        staffApplications.requests=(Array.isArray(data)?data:[]).slice().sort((a,b)=>donationPriority(a)-donationPriority(b)||String(a.bloodNeededDate||'').localeCompare(String(b.bloodNeededDate||''))||Number(b.id)-Number(a.id));
        selectStaffRequestTab(staffApplications.requestTab);
        watchRequestDate($('#applicationRequests'),()=>selectStaffRequestTab(staffApplications.requestTab));
    }catch(error){if(staffDashboardIsCurrent(generation)){$('#applicationRequests').innerHTML='<div class="empty">Unable to load your hospital’s requests. Use Refresh to try again.</div>';notify(error.message,true);}}
}
function drawStaffRequestList(){
    const counts={active:staffApplications.requests.filter(requestIsAvailable).length,general:staffApplications.requests.length,completed:staffApplications.requests.filter(r=>r.bloodRequestStatus==='COMPLETED').length};
    const labels={active:'Active Requests',general:'General History',completed:'Completed History'};
    $('#applicationRequestTabs').innerHTML='<div class="record-tabs" role="group" aria-label="Hospital donation request views">'+['active','general','completed'].map(tab=>'<button type="button" class="record-tab '+(staffApplications.requestTab===tab?'selected':'')+'" data-application-tab="'+tab+'" aria-pressed="'+(staffApplications.requestTab===tab)+'">'+labels[tab]+' <span>'+counts[tab]+'</span></button>').join('')+'</div>';
    document.querySelectorAll('[data-application-tab]').forEach(b=>b.onclick=()=>selectStaffRequestTab(b.dataset.applicationTab));
    const visible=staffApplications.requests.filter(r=>staffApplications.requestTab==='active'?requestIsAvailable(r):staffApplications.requestTab==='completed'?r.bloodRequestStatus==='COMPLETED':true);
    $('#applicationRequests').innerHTML=visible.length?visible.map(r=>'<button type="button" class="application-request '+(String(r.id)===String(staffApplications.requestId)?'selected':'')+'" data-application-request="'+esc(r.id)+'" aria-pressed="'+(String(r.id)===String(staffApplications.requestId))+'"><span class="split"><strong>'+esc(r.requiredBloodType||'Blood request')+' <small>Request #'+esc(r.id)+'</small></strong><span class="pill '+(/HIGH|CRITICAL/.test(r.bloodRequestUrgency)?'red':'')+'">'+esc(r.bloodRequestUrgency||'Not available')+'</span></span><span class="application-request-meta">'+esc(r.requiredBloodUnits??'—')+' units · Needed '+esc(r.bloodNeededDate||'Not available')+'</span><span class="meta">'+statusPill(r.bloodRequestStatus)+(r.bloodNeededDate&&r.bloodNeededDate<localToday()?'<span class="pill amber">Expired</span>':'')+'</span></button>').join(''):'<div class="empty">'+(staffApplications.requestTab==='active'?'No active blood requests at this time.':staffApplications.requestTab==='completed'?'No completed blood requests yet.':'Your hospital has no blood requests yet.')+'</div>';
    document.querySelectorAll('[data-application-request]').forEach(b=>b.onclick=()=>selectStaffRequest(b.dataset.applicationRequest));
}
function selectStaffRequestTab(tab){
    staffApplications.requestTab=tab;
    const visible=staffApplications.requests.filter(r=>tab==='active'?requestIsAvailable(r):tab==='completed'?r.bloodRequestStatus==='COMPLETED':true);
    const selected=visible.find(r=>String(r.id)===String(staffApplications.requestId));
    drawStaffRequestList();
    if(selected)return;
    if(visible.length){selectStaffRequest(visible[0].id);return;}
    staffApplications.selection++;
    staffApplications.requestId=null;staffApplications.bookingId=null;staffApplications.bookings=[];
    $('#applicationStats').innerHTML='';
    $('#applicationRequestContext').innerHTML='';
    $('#applicationBookings').innerHTML='<div class="empty">Choose a blood request to review its donation applications.</div>';
    $('#applicationDonorDetails').innerHTML='<div class="empty">Select an application to review donor details.</div>';
}
async function selectStaffRequest(id,preferredBookingId){
    if(state.role!=='HOSPITAL_STAFF'||!state.token)return;
    let request=staffApplications.requests.find(r=>String(r.id)===String(id));
    if(!request)return; // Only IDs obtained from the staff member’s own hospital list.
    const generation=staffApplications.generation,selection=++staffApplications.selection;
    staffApplications.requestId=request.id;staffApplications.bookingId=null;staffApplications.bookings=[];
    drawStaffRequestList();
    $('#applicationStats').innerHTML='';
    $('#applicationRequestContext').innerHTML='<div class="panel-head"><div><h2>Applications · Request #'+esc(request.id)+'</h2><p class="hint">'+esc(request.requiredBloodType||'Not available')+' · '+esc(request.bloodRequestDescription||'No request description provided.')+'</p></div>'+statusPill(request.bloodRequestStatus)+'</div>';
    $('#applicationBookings').innerHTML='<p class="muted">Loading donation applications…</p>';
    $('#applicationDonorDetails').innerHTML='<div class="empty">Select an application to review donor details.</div>';
    try{
        const data=await api('/api/donation-bookings/blood-request/'+encodeURIComponent(request.id));
        // Re-read request statuses: loading applications can follow a completed donation.
        const requests=await api('/api/blood-requests/hospital');
        if(!staffDashboardIsCurrent(generation)||selection!==staffApplications.selection)return;
        staffApplications.requests=(Array.isArray(requests)?requests:[]).slice().sort((a,b)=>donationPriority(a)-donationPriority(b)||String(a.bloodNeededDate||'').localeCompare(String(b.bloodNeededDate||''))||Number(b.id)-Number(a.id));
        request=staffApplications.requests.find(r=>String(r.id)===String(id));
        const visible=request&&(staffApplications.requestTab==='active'?requestIsAvailable(request):staffApplications.requestTab==='completed'?request.bloodRequestStatus==='COMPLETED':true);
        if(!visible){selectStaffRequestTab(staffApplications.requestTab);return;}
        drawStaffRequestList();
    $('#applicationRequestContext').innerHTML='<div class="panel-head"><div><h2>Applications · Request #'+esc(request.id)+'</h2><p class="hint">'+esc(request.requiredBloodType||'Not available')+' · '+esc(request.bloodRequestDescription||'No request description provided.')+'</p></div>'+statusPill(request.bloodRequestStatus)+'</div>';
        staffApplications.bookings=(Array.isArray(data)?data:[]).filter(b=>String(b.bloodRequestId)===String(request.id));
        drawStaffBookingList();
        const preferred=staffApplications.bookings.find(b=>String(b.id)===String(preferredBookingId));
        if(preferred||staffApplications.bookings.length)selectStaffDonor((preferred||staffApplications.bookings[0]).id);
    }catch(error){if(staffDashboardIsCurrent(generation)&&selection===staffApplications.selection){$('#applicationBookings').innerHTML='<div class="empty">Applications could not be loaded. Select the request again to retry.</div>';notify(error.message,true);}}
}
function drawStaffBookingList(){
    const bookings=staffApplications.bookings;
    $('#applicationStats').innerHTML='<div class="stats application-stats">'+[['Hospital requests',staffApplications.requests.length],['Applications for this request',bookings.length],['Pending review',bookings.filter(b=>b.donationBookingStatus==='PENDING').length]].map(([label,count])=>'<div class="stat"><p>'+label+'</p><strong>'+count+'</strong></div>').join('')+'</div>';
    $('#applicationBookings').innerHTML=bookings.length?'<div class="application-booking-list">'+bookings.map(b=>'<button type="button" class="application-booking '+(String(b.id)===String(staffApplications.bookingId)?'selected':'')+'" data-application-booking="'+esc(b.id)+'" aria-pressed="'+(String(b.id)===String(staffApplications.bookingId))+'"><span><strong>'+detailValue(b.donorName)+'</strong><small>Email: '+detailValue(b.donorEmail)+' · Phone: '+detailValue(b.donorPhone)+' · Blood type: '+detailValue(b.bloodType)+'</small><small>Booking #'+esc(b.id)+' · '+esc(b.donationBookingDate||'Not available')+' · '+esc(b.donationBookingTime||'Not available')+'</small></span>'+statusPill(b.donationBookingStatus)+'</button>').join('')+'</div>':'<div class="empty">No donation applications for this request yet. Bookings will appear here when donors reserve an appointment.</div>';
    document.querySelectorAll('[data-application-booking]').forEach(b=>b.onclick=()=>selectStaffDonor(b.dataset.applicationBooking));
}
function selectStaffDonor(id){
    if(state.role!=='HOSPITAL_STAFF'||!state.token)return;
    const booking=staffApplications.bookings.find(b=>String(b.id)===String(id));
    if(!booking)return;
    staffApplications.bookingId=booking.id;drawStaffBookingList();
    // Slot and booking details come from this application response, so historical bookings do not depend on the cached slot list.
    const donor={};
    const availability=typeof donor.availableForBloodDonation==='boolean'?donor.availableForBloodDonation:donor.isAvailableForBloodDonation;
    const transitions=bookingTransitions(booking.donationBookingStatus);
    $('#applicationDonorDetails').innerHTML='<div class="donor-detail-heading"><div><span class="eyebrow">DONOR APPLICATION</span><h2>'+detailValue(booking.donorName)+'</h2><p class="hint">Booking #'+esc(booking.id)+'</p></div>'+statusPill(booking.donationBookingStatus)+'</div>'
        +detailSection('Personal Information',[['Full name',booking.donorName],['Email',booking.donorEmail],['Phone',booking.donorPhone],['Date of birth',donor.dateOfBirth],['Gender',donor.gender],['Blood type',booking.bloodType],['Donation availability',typeof availability==='boolean'?(availability?'Available':'Unavailable'):null]])
        +detailSection('Medical Information',[['Weight (kg)',donor.weightKg],['Medical conditions',donor.medicalConditions],['Medications',donor.medications],['Allergies',donor.allergies],['Donor notes',donor.notesAboutTheDonor]])
        +detailSection('Donation History',[['Previous donation date',donor.lastDonationDate],['Total donations',donor.totalDonations]])
        +detailSection('Booking Details',[['Booking date',booking.donationBookingDate],['Booking time',booking.donationBookingTime],['Status',booking.donationBookingStatus],['Booking notes',booking.bookingNotes],['Appointment slot ID',booking.donationSlotId],['Slot date',booking.slotDate],['Slot start time',booking.slotStartTime],['Slot end time',booking.slotEndTime],['Slot status',booking.slotStatus],['Slot capacity',booking.slotCapacity],['Reserved places',booking.reservedPlaces],['Application received',booking.applicationReceived],['Last updated',booking.lastUpdated]])
        +'<p class="hint document-access-note">The application API provides donor name, email, phone and blood type. Other personal details, medical history and documents are not available for staff review.</p>'
        +(transitions.length?'<form id="applicationStatusForm" class="application-status-form"><label for="applicationNextStatus">Update booking status</label><div class="actions"><select id="applicationNextStatus" required aria-label="New booking status"><option value="">Choose an action</option>'+transitions.map(s=>'<option value="'+s+'">'+({CONFIRMED:'Confirm appointment',COMPLETED:'Mark donation completed',CANCELLED:'Cancel appointment'})[s]+'</option>').join('')+'</select><button class="button" type="submit">Save status</button></div></form>':'<p class="hint">'+(['COMPLETED','CANCELLED'].includes(booking.donationBookingStatus)?'This booking is final and cannot be changed.':'No status changes are available for this booking.')+'</p>');
    if(transitions.length)$('#applicationStatusForm').onsubmit=e=>updateStaffApplicationStatus(e,booking);
}
async function updateStaffApplicationStatus(event,booking){
    event.preventDefault();
    if(state.role!=='HOSPITAL_STAFF'||!state.token||!staffApplications.bookings.includes(booking))return;
    const next=val('applicationNextStatus');
    if(!bookingTransitions(booking.donationBookingStatus).includes(next))return;
    if(!confirm('Change booking #'+booking.id+' to '+next+'?'))return;
    const generation=staffApplications.generation,selection=staffApplications.selection,requestId=staffApplications.requestId;
    const button=event.target.querySelector('button');button.disabled=true;
    try{
        await api('/api/donation-bookings/'+encodeURIComponent(booking.id)+'/status?newStatus='+encodeURIComponent(next),{method:'PUT'});
        if(!staffDashboardIsCurrent(generation)||selection!==staffApplications.selection)return;
        await selectStaffRequest(requestId,booking.id);
        if(staffDashboardIsCurrent(generation)&&String(staffApplications.requestId)===String(requestId))notify('Booking status updated successfully.');
    }catch(error){if(staffDashboardIsCurrent(generation)&&selection===staffApplications.selection){notify(error.message,true);button.disabled=false;}}
}

function newestDonorBookings(data){
    const createdAt=booking=>Date.parse(booking.createdAt||booking.createdAtDonationBooking||'');
    // Use one consistent ordering for the whole list; partial timestamps fall back to IDs.
    const hasCreationDates=data.every(booking=>Number.isFinite(createdAt(booking)));
    return data.slice().sort((a,b)=>(hasCreationDates?createdAt(b)-createdAt(a):0)||(Number(b.id)-Number(a.id)));
}
function bookingCards(data, staff) {
    if(!staff)data=newestDonorBookings(data);
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
                            <button class="button" data-reminder="${b.id}" data-booking-date="${esc(b.donationBookingDate)}" data-booking-time="${esc(b.donationBookingTime)}">Set reminder</button>
                            <button class="button secondary" data-cancel="${b.id}">
                                Cancel booking
                            </button>
                        `;
            }
        }

        if(!staff){
            const request=state.requests.find(r=>String(r.id)===String(b.bloodRequestId));
            const slot=state.slots.find(s=>String(s.id)===String(b.donationSlotId));
            const hospitalId=request?.hospitalId??slot?.hospitalId;
            const hospital=state.hospitals.find(h=>hospitalId!=null&&String(h.id)===String(hospitalId));
            const hospitalName=hospital?.hospitalName||(hospitalId!=null?'Hospital #'+hospitalId:'Not available');
            const statusClass=({PENDING:'pending',CONFIRMED:'confirmed',COMPLETED:'completed',CANCELLED:'cancelled'})[status]||'unknown';
            return `
                <article class="item appointment-card">
                    <div class="appointment-card-head">
                        <span class="appointment-reference">Booking #${esc(b.id)}</span>
                        <span class="pill appointment-status ${statusClass}">${esc(status||'Unknown')}</span>
                    </div>
                    <div class="appointment-heading">
                        <span class="appointment-symbol" aria-hidden="true">${navIcon('slots')}</span>
                        <div><span class="eyebrow">BLOOD DONATION</span><h3>Donation appointment</h3></div>
                    </div>
                    <dl class="appointment-facts">
                        <div><dt>Appointment date</dt><dd><time datetime="${esc(b.donationBookingDate)}">${detailValue(b.donationBookingDate)}</time></dd></div>
                        <div><dt>Appointment time</dt><dd>${detailValue(b.donationBookingTime)}</dd></div>
                        <div class="appointment-hospital"><dt>Hospital</dt><dd>${esc(hospitalName)}</dd></div>
                    </dl>
                    <p class="appointment-request">Blood request #${esc(b.bloodRequestId)}</p>
                    ${b.notesAboutTheDonationBooking?'<p class="appointment-notes">'+esc(b.notesAboutTheDonationBooking)+'</p>':''}
                    ${actions?'<div class="actions appointment-actions">'+actions+'</div>':'<p class="appointment-final">This appointment is '+esc(String(status).toLowerCase())+'.</p>'}
                </article>`;
        }

        return `
                    <div class="item">
                        <div class="split">
                            <span class="pill">Booking #${esc(b.id)}</span>
                            ${statusPill(status)}
                        </div>

                        <h3>
                            ${esc('Donation appointment · Request #'+b.bloodRequestId)}
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

function reminderForm(bookingId,bookingDate,bookingTime){
    $('#view').innerHTML=panel('Set your donation reminder',`<div class="context-strip"><span class="context-symbol">◷</span><div><strong>Booking #${esc(bookingId)}</strong><p>Your appointment is ${esc(bookingDate)} at ${esc(bookingTime)}</p></div></div><form id="reminderForm"><div class="field"><label for="reminderAt">Remind me on</label><input id="reminderAt" type="datetime-local" required></div><p class="hint">Choose a time in the future, before your appointment. Your reminder will appear in Notifications.</p><div class="actions"><button class="button">Save reminder</button><button class="button secondary" type="button" id="backBookings">Back to bookings</button></div></form>`);
    $('#backBookings').onclick=bookings;
    const now=new Date();now.setMinutes(now.getMinutes()+1);const localValue=new Date(now.getTime()-now.getTimezoneOffset()*60000).toISOString().slice(0,16);$('#reminderAt').min=localValue;$('#reminderAt').max=(bookingDate+'T'+bookingTime).slice(0,16);
    $('#reminderForm').onsubmit=async e=>{e.preventDefault();const v=val('reminderAt');const at=new Date(v),appointment=new Date(bookingDate+'T'+bookingTime);if(!Number.isFinite(at.getTime())||at<=new Date()||at>=appointment){notify('Choose a reminder time in the future and before your appointment.',true);return;}const btn=e.target.querySelector('button');btn.disabled=true;try{await api('/api/donation-reminders?'+new URLSearchParams({bookingId,reminderTime:v.length===16?v+':00':v}),{method:'POST'});await bookings();notify('Reminder scheduled. You will see it in Notifications when it is due.');}catch(err){notify(err.message,true);btn.disabled=false;}};
}
function wireBookingButtons(){document.querySelectorAll('[data-reminder]').forEach(b=>b.onclick=()=>reminderForm(b.dataset.reminder,b.dataset.bookingDate,b.dataset.bookingTime));document.querySelectorAll('[data-cancel]').forEach(b=>b.onclick=async()=>{if(!confirm('Cancel this booking?'))return;try{await api(`/api/donation-bookings/${b.dataset.cancel}/cancel`,{method:'PUT'});notify('Booking cancelled.');bookings()}catch(e){notify(e.message,true)}});document.querySelectorAll('[data-booking-status]').forEach(s=>s.onchange=async()=>{if(!s.value)return;if(!confirm(`Update booking to ${s.value}?`))return;try{await api(`/api/donation-bookings/${s.dataset.bookingStatus}/status?newStatus=${encodeURIComponent(s.value)}`,{method:'PUT'});notify('Booking status updated.');$('#findBookings').requestSubmit()}catch(e){notify(e.message,true)}})}
let profileVisit=0;
function sessionProfileEmail(){
    // Display-only subject. Permissions are always enforced by the server.
    try{
        const payload=state.token.split('.')[1];
        const base64=payload.replace(/-/g,'+').replace(/_/g,'/');
        const bytes=Uint8Array.from(atob(base64.padEnd(Math.ceil(base64.length/4)*4,'=')),c=>c.charCodeAt(0));
        const claims=JSON.parse(new TextDecoder().decode(bytes));
        return typeof claims.sub==='string'?claims.sub:null;
    }catch{return null;}
}
function profileFields(fields){return '<dl class="donor-fields profile-fields">'+fields.map(([label,value])=>'<div><dt>'+esc(label)+'</dt><dd>'+detailValue(value)+'</dd></div>').join('')+'</dl>';}
function profileBoolean(value,yes='Yes',no='No'){return typeof value==='boolean'?(value?yes:no):null;}
async function profile(){
    if(!state.token){navigate('auth');return;}
    stopRequestDateWatch();stopSlotExpiryWatch();
    const visit=++profileVisit,token=state.token,role=state.role,email=sessionProfileEmail();
    const current=()=>visit===profileVisit&&state.token===token&&state.role===role&&!!$('#profileOverview');
    let account={},donor={},hospital={},notice='',savedDetails={};
    const workspace=({DONOR:'Donor',HOSPITAL_STAFF:'Hospital staff',ADMIN:'Administrator'})[role]||'Not available';
    $('#view').innerHTML='<div class="section-intro profile-intro"><div><span class="eyebrow">YOUR ACCOUNT & CARE INFORMATION</span><h2>My Profile</h2><p>Review your available account details and manage your personal settings.</p></div><button class="button secondary" type="button" id="refreshProfile">Refresh information</button></div><section class="panel profile-overview" id="profileOverview" aria-label="Profile Overview" aria-busy="true"><p class="muted">Loading your account information…</p></section><div class="profile-layout"><div class="profile-information"><section class="panel"><div class="panel-head"><h2>Personal Information</h2></div><div id="profilePersonal"><p class="muted">Loading available details…</p></div><details class="profile-edit" open><summary>Edit name and phone number</summary><form id="profileForm"><div class="form-grid">'+field('profileName','Full name','text','autocomplete="name" required')+field('profilePhone','Phone number','tel','autocomplete="tel" required')+'</div><button class="button" type="submit">Save changes</button></form></details></section><section class="panel" id="profileRoleSection"><div class="panel-head"><h2>'+ (role==='DONOR'?'Donor Information':role==='HOSPITAL_STAFF'?'Staff Information':'Administrator Information')+'</h2></div><div id="profileRoleInformation"><p class="muted">Loading available information…</p></div></section></div><section class="panel profile-settings"><div class="panel-head"><h2>Account Settings</h2></div><section class="profile-setting-section"><h3>Change password</h3><p class="hint">Use your current password to set a new password.</p><form id="passwordForm">'+field('oldPassword','Current password','password','autocomplete="current-password" required')+field('newPassword','New password','password','autocomplete="new-password" required')+'<button class="button" type="submit">Change password</button></form></section><section class="profile-setting-section" aria-labelledby="myDocumentsTitle"><h3 id="myDocumentsTitle">My Documents</h3><div class="profile-data-note" id="profileCprStatus" role="status" aria-live="polite"><strong>Existing CPR document: Not available</strong><br>The current service supports uploading CPR documents but does not provide authorized viewing or downloading. This does not indicate whether a document is already stored.</div><form id="cprForm"><label for="profileCprFile">CPR document · PDF, JPEG or PNG · up to 5 MB</label><input id="profileCprFile" type="file" name="file" accept=".pdf,.jpg,.jpeg,.png" required><button class="button secondary" type="submit">Upload CPR document</button></form></section><section class="profile-setting-section" aria-labelledby="profilePhotoTitle"><h3 id="profilePhotoTitle">Profile Photo</h3><div class="profile-data-note" id="profilePhotoStatus" role="status" aria-live="polite"><strong>Existing profile picture: Not available</strong><br>The current service does not provide an authorized image retrieval endpoint. Your initials or the medical symbol are shown as an account icon, not an uploaded photo.</div><form id="imageForm"><label for="profileImageFile">Profile photo · JPEG or PNG · up to 5 MB</label><input id="profileImageFile" type="file" name="file" accept=".jpg,.jpeg,.png" required><button class="button secondary" type="submit">Upload profile photo</button></form></section><section class="profile-setting-section profile-deactivation"><h3>Deactivate account</h3><p class="hint">Deactivation disables your account and your ability to sign in.</p><button class="button outline" type="button" id="deactivateMe">Deactivate my account</button></section></section></div>';
    $('#refreshProfile').onclick=profile;
    function drawInformation(){
        if(!current())return;
        $('#profileOverview').setAttribute('aria-busy','false');
        $('#profileOverview').innerHTML='<div class="profile-identity"><div class="profile-avatar" aria-hidden="true">'+esc(account.name?Array.from(account.name.trim())[0].toUpperCase():'✚')+'</div><div><span class="eyebrow">PROFILE OVERVIEW</span><h2>'+esc(account.name||'Name not available')+'</h2><p>'+detailValue(account.email||email)+'</p></div><span class="pill red">Workspace: '+esc(workspace)+'</span></div>'+profileFields([['Full name',account.name],['Email',account.email||email],['Phone number',account.phoneNumber],['Account role',account.role],['Account status',account.status],['Email verified',profileBoolean(account.emailVerified)]])+'<div class="profile-data-note" role="status">'+esc(notice)+'</div>';
        $('#profilePersonal').innerHTML=profileFields([['Full name',account.name],['Email',account.email||email],['Phone number',account.phoneNumber],['Account ID',account.id],['Account created',account.createdAtUser],['Last account update',account.updatedAtUser]]);
        if(role==='DONOR'){
            const availability=typeof donor.availableForBloodDonation==='boolean'?donor.availableForBloodDonation:donor.isAvailableForBloodDonation;
            $('#profileRoleInformation').innerHTML=profileFields([['Blood type',donor.bloodType],['Date of birth',donor.dateOfBirth],['Gender',donor.gender],['Weight (kg)',donor.weightKg],['Donation availability',profileBoolean(availability,'Available','Unavailable')],['Last donation date',donor.lastDonationDate],['Total donations',donor.totalDonations],['Medical conditions',donor.medicalConditions],['Medications',donor.medications],['Allergies',donor.allergies],['Donor notes',donor.notesAboutTheDonor],['Donor profile created',donor.createdAtDonorProfile],['Donor profile updated',donor.updatedAtDonorProfile]]);
        }else if(role==='HOSPITAL_STAFF'){
            $('#profileRoleInformation').innerHTML=profileFields([['Staff name',account.name],['Staff email',account.email||email],['Staff phone',account.phoneNumber],['Account role',account.role],['Hospital',hospital.hospitalName],['Hospital ID',hospital.id],['Hospital location',hospital.hospitalLocation],['Hospital phone',hospital.hospitalPhone],['Hospital status',profileBoolean(hospital.hospitalActive,'Active','Inactive')],['Hospital description',hospital.hospitalDescription]]);
        }else{
            $('#profileRoleInformation').innerHTML='<p class="hint">This section displays your own account information only.</p>'+profileFields([['Administrator name',account.name],['Email',account.email||email],['Phone number',account.phoneNumber],['Account role',account.role]]);
        }
    }
    $('#profileForm').onsubmit=async event=>{
        event.preventDefault();const name=val('profileName').trim(),phoneNumber=val('profilePhone').trim();
        if(!name||!phoneNumber){notify('Enter your full name and phone number.',true);return;}
        const button=event.target.querySelector('button');button.disabled=true;
        try{await api('/api/users/profile',{method:'PUT',body:JSON.stringify({name,phoneNumber})});
            if(current()){savedDetails={name,phoneNumber};account={...account,...savedDetails};drawInformation();notify('Profile updated. Your saved name and phone are shown for this visit.');}
        }catch(error){if(current())notify(error.message,true);}finally{if(current())button.disabled=false;}
    };
    $('#passwordForm').onsubmit=async event=>{
        event.preventDefault();const button=event.target.querySelector('button');button.disabled=true;
        try{await api('/api/auth/change-password',{method:'PUT',body:JSON.stringify({currentPassword:val('oldPassword'),newPassword:val('newPassword')})});if(current()){event.target.reset();notify('Password updated successfully.');}}
        catch(error){if(current())notify(error.message,true);}finally{if(current())button.disabled=false;}
    };
    for(const [form,path] of [['cprForm','/api/users/profile/cpr'],['imageForm','/api/users/profile/image']]){
        $('#'+form).onsubmit=async event=>{
            event.preventDefault();const button=event.target.querySelector('button');button.disabled=true;
            const status=$('#'+(form==='cprForm'?'profileCprStatus':'profilePhotoStatus'));
            const previousStatus=status.innerHTML;
            try{
                const file=event.target.querySelector('input[type=file]').files[0];
                if(!file)throw Error('Choose a file to upload.');
                if(file.size>5*1024*1024)throw Error('Maximum file size is 5 MB.');
                const types=form==='cprForm'?['application/pdf','image/jpeg','image/png']:['image/jpeg','image/png'];
                if(!types.includes(file.type))throw Error(form==='cprForm'?'Choose a PDF, JPEG or PNG document.':'Choose a JPEG or PNG profile photo.');
                status.textContent='Uploading your '+(form==='cprForm'?'CPR document':'profile photo')+'…';
                const body=new FormData();body.append('file',file);
                await api(path,{method:'POST',body});
                if(current()){
                    event.target.reset();
                    // A receipt for this successful upload; never claim a retrieved file or preview.
                    status.textContent='Uploaded successfully during this page visit: '+file.name+'. Viewing and downloading saved files are not supported by the current service.';
                    notify((form==='cprForm'?'CPR document':'Profile photo')+' uploaded successfully. Saved-file viewing is not available.');
                }
            }catch(error){if(current()){status.innerHTML=previousStatus;notify(error.message,true);}}
            finally{if(current())button.disabled=false;}
        };
    }
    $('#deactivateMe').onclick=async()=>{
        if(!confirm('Deactivate your account? This action affects your ability to sign in.'))return;
        try{await api('/api/users/profile',{method:'DELETE'});if(current()){signout();notify('Account deactivated.');}}catch(error){if(current())notify(error.message,true);}
    };
    try{
        if(role==='DONOR'){
            const data=await api('/api/donation-bookings/donor');if(!current())return;
            const bookings=Array.isArray(data)?data:[];
            // Never use another account’s nested user details, even if returned unexpectedly.
            const profiles=bookings.map(b=>b.donorProfile).filter(p=>p&&email&&p.user?.email===email);
            profiles.sort((a,b)=>String(b.updatedAtDonorProfile||'').localeCompare(String(a.updatedAtDonorProfile||'')));
            donor=profiles[0]||{};account=donor.user||{};
            notice=profiles.length?'Available details were returned with your own donation bookings. Email may also be shown from your signed-in session.':'Your account and donor details are not available through the current read APIs. Your signed-in email is shown when available; other fields are marked Not available.';
        }else if(role==='HOSPITAL_STAFF'){
            const data=await api('/api/blood-requests/hospital');if(!current())return;
            const requests=Array.isArray(data)?data:[];
            const hospitalId=requests.find(r=>r.hospitalId!=null)?.hospitalId;
            hospital=hospitalId==null?{}:await api('/api/hospitals/'+encodeURIComponent(hospitalId));
            if(!current())return;
            notice='Hospital information comes from your hospital’s blood requests. The current API does not expose your staff name, phone or account details. Your email is shown from your signed-in session.';
        }else{
            notice='The current API does not expose an administrator self-profile. Your signed-in email is shown when available; other account fields are marked Not available.';
        }
        if(!current())return;
        account={...account,...savedDetails};
        drawInformation();
        if(account.name&&!val('profileName'))$('#profileName').value=account.name;
        if(account.phoneNumber&&!val('profilePhone'))$('#profilePhone').value=account.phoneNumber;
    }catch(error){
        if(!current())return;
        notice='Unable to load profile information. Your account controls remain available. Use Refresh information to try again.';
        drawInformation();notify(error.message,true);
    }
}
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
                        <div class="item ${n.isRead?'':'notification-unread'}" style="margin-top:10px">
                            <strong>
                                ${esc(n.notificationMessage || 'Booking updated')} ${statusPill(n.isRead?'READ':'NEW')}
                            </strong>
                            <p>
                                ${esc(n.notificationTime ? new Date(n.notificationTime).toLocaleString() : '')}
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
        notify(error.message,true);
    }
}
function auth(){if(state.token){navigate('overview');return}$('#view').innerHTML=`<div class="auth-layout"><div class="auth-tabs"><button class="button" id="tabLogin">Sign in</button><button class="button secondary" id="tabRegister">Create account</button><button class="button secondary" id="tabForgot">Forgot password</button></div><div id="authContent"></div></div>`;$('#tabLogin').onclick=showLogin;$('#tabRegister').onclick=showRegister;$('#tabForgot').onclick=showForgot;showLogin()}
function showLogin(){$('#authContent').innerHTML=panel('Welcome back',`<form id="loginForm">${field('email','Email','email','required')}${field('password','Password','password','required')}${select('loginRole','Your role',['DONOR','HOSPITAL_STAFF','ADMIN'])}<p class="hint">Select the role assigned to your account. The server still enforces actual permissions.</p><button class="button" style="width:100%">Sign in</button></form>`);$('#loginForm').onsubmit=async e=>{e.preventDefault();try{const d=await api('/api/auth/login',{method:'POST',body:JSON.stringify({email:val('email'),password:val('password')})});const token=d.token||d.jwtToken||d.accessToken;if(!token)throw Error('Login succeeded but the response has no recognized token field.');state.token=token;state.role=val('loginRole');sessionStorage.setItem('med_token',token);sessionStorage.setItem('med_role',state.role);connectSocket();
    navigate('overview');
    notify('Signed in successfully.')}catch(err){notify(err.message,true)}}}
function showRegister(){$('#authContent').innerHTML=panel('Join Med Support',`<form id="registerForm">${field('rName','Full name','text','required')}${field('rEmail','Email','email','required')}${field('rPhone','Phone number','tel','required')}${field('rPassword','Password','password','required')}${select('rRole','Account type',['DONOR','HOSPITAL_STAFF'])}<button class="button">Create account</button></form><p class="hint">Verify your email before signing in. Hospital staff may require additional approval.</p>`);$('#registerForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/auth/register',{method:'POST',body:JSON.stringify({name:val('rName'),email:val('rEmail'),phoneNumber:val('rPhone'),password:val('rPassword'),role:val('rRole')})});notify('Registration submitted. Check your email for verification.');showLogin()}catch(err){notify(err.message,true)}}}
function showForgot(){$('#authContent').innerHTML=panel('Recover your account',`<form id="forgotForm">${field('forgotEmail','Email address','email','required')}<button class="button">Send reset link</button></form>`);$('#forgotForm').onsubmit=async e=>{e.preventDefault();try{await api('/api/auth/forgot-password?email='+encodeURIComponent(val('forgotEmail')),{method:'POST'});notify('Password reset requested. Check your email.')}catch(err){notify(err.message,true)}}}
function signout(){state.token='';state.role='GUEST';state.notifications=[];sessionStorage.removeItem('med_token');sessionStorage.removeItem('med_role');if(state.socket){try{state.socket.deactivate()}catch{}state.socket=null}navigate('auth')}
function connectSocket(){if(!state.token||!window.StompJs)return;try{const client=new StompJs.Client({connectHeaders: {
        Authorization: 'Bearer ' + state.token
    },webSocketFactory:()=>new SockJS('/ws'),reconnectDelay:5000,onConnect:()=>{client.subscribe('/user/queue/notifications',msg=>{let data;try{data=JSON.parse(msg.body)}catch{data=msg.body}const message=typeof data==='string'?data:(data&&typeof data==='object'?(data.notificationMessage||data.message):'');loadNotificationCount();if(state.page==='notifications')notifications().catch(console.error);if(typeof message==='string'&&message.trim())notify(message);})}});client.activate();state.socket=client}catch(e){console.warn('WebSocket connection unavailable:',e)}}
$('#logout').onclick=signout;const params=new URLSearchParams(location.search);if(params.has('token')){api('/api/auth/verify-email?token='+encodeURIComponent(params.get('token'))).then(()=>notify('Email verified. You can now sign in.')).catch(e=>notify(e.message,true))}if (state.token) {
    navigate('overview');
    connectSocket();
} else {
    navigate('auth');
}
