/* Project 4: Identity sessions/donor profiles and donor blood request browsing are configured. */
const $=s=>document.querySelector(s), esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const state={token:'',email:'',role:'GUEST',user:null,donorProfileId:null,bookingContext:null,page:'overview',hospitals:[],requests:[],bookings:[],notifications:[],socket:null,slots:[]};
let sessionGeneration=0;
let profileVisit=0;
let notificationReconnect=null, notificationSocketToken='';
const pages={shared:['','Shared blood request'],overview:['⌂','Overview'],hospitals:['✚','Hospitals'],requests:['♡','Blood Requests'],slots:['◷','Donation Slots'],bookings:['▦','Donations'],profile:['♙','My Profile'],notifications:['N','Notifications'],departments:['D','Departments'],admin:['⚙','Administration'],auth:['⇥','Sign in']};
function notify(msg,error=false){$('#notice').textContent=msg;$('#notice').className=error?'error':'';window.scrollTo({top:0,behavior:'smooth'});}

function resolveApiRoute(path,method='GET'){
    if(typeof path!=='string'||!path.startsWith('/api/')||path.includes('#'))throw requestError(0,'Unsupported Project 4 API action. This feature is not connected.');
    const route=path.split('?')[0],verb=String(method).toUpperCase();
    const routes=[
        ['identity',/^\/api\/auth\/(?:login|register|forgot-password|reset-password)$/,['POST'],true],
        ['identity',/^\/api\/auth\/verify-email$/,['GET'],true],
        ['identity',/^\/api\/auth\/change-password$/,['PUT']],
        ['identity',/^\/api\/users\/me$/,['GET','PUT','DELETE']],
        ['identity',/^\/api\/users\/profile\/(?:cpr|image)$/,['POST']],
        ['identity',/^\/api\/donor-profile$/,['POST']],
        ['identity',/^\/api\/donor-profile\/me$/,['GET','PUT']],
        ['identity',/^\/api\/users\/staff$/,['POST']],
        ['identity',/^\/api\/users$/,['GET']],
        ['identity',/^\/api\/users\/\d+\/(?:activate|reactivate|deactivate)$/,['PUT']],
        ['notification',/^\/api\/notifications\/my$/,['GET']],
        ['bloodRequest',/^\/api\/hospitals\/\d+\/departments$/,['GET','POST']],
        ['identity',/^\/api\/users\/\d+\/hospital\/\d+$/,['PUT']],
        ['bloodRequest',/^\/api\/hospitals(?:\/\d+)?$/,['GET'],true],
        ['bloodRequest',/^\/api\/hospitals$/,['POST']],
        ['bloodRequest',/^\/api\/hospitals\/\d+$/,['PUT']],
        ['bloodRequest',/^\/api\/hospitals\/\d+\/(?:activate|deactivate)$/,['PATCH']],
        ['bloodRequest',/^\/api\/blood-requests\/shared\/\d+$/,['GET'],true],
        ['bloodRequest',/^\/api\/blood-requests$/,['GET','POST']],
        ['bloodRequest',/^\/api\/blood-requests\/hospital$/,['GET']],
        ['bloodRequest',/^\/api\/blood-requests\/progress$/,['GET']],
        ['bloodRequest',/^\/api\/blood-requests\/browse$/,['GET']],
        ['bloodRequest',/^\/api\/blood-requests\/open(?:\/(?:filter|\d+))?$/,['GET']],
        ['bloodRequest',/^\/api\/blood-requests\/\d+\/status$/,['PATCH']],
        ['bloodRequest',/^\/api\/blood-requests\/\d+$/,['GET','PUT','DELETE']],
        ['booking',/^\/api\/donation-slots\/(?:available|my-hospital|my-history)$/,['GET']],
        ['booking',/^\/api\/donation-slots\/my-hospital\/lifecycle$/,['GET']],
        ['booking',/^\/api\/donation-slots$/,['POST']],
        ['booking',/^\/api\/donation-slots\/\d+\/capacity$/,['PUT']],
        ['booking',/^\/api\/donation-bookings$/,['POST']],
        ['booking',/^\/api\/donation-bookings\/my$/,['GET']],
        ['booking',/^\/api\/donation-bookings\/(?:hospital-summary|my-hospital)$/,['GET']],
        ['booking',/^\/api\/donation-bookings\/\d+$/,['DELETE']],
        ['booking',/^\/api\/donation-bookings\/blood-request\/\d+$/,['GET']],
        ['booking',/^\/api\/donation-bookings\/\d+\/status$/,['PUT']],
        ['booking',/^\/api\/donation-bookings\/\d+\/reschedule$/,['PUT']]
    ];
    const match=routes.find(([,pattern,methods])=>pattern.test(route)&&methods.includes(verb));
    if(!match)throw requestError(0,'Unsupported Project 4 API action. This feature is not connected.');
    return {service:match[0],public:match[3]===true};
}
async function api(path,opts={}){
    const route=resolveApiRoute(path,opts.method||'GET');
    return route.public?serviceRequest(route.service,path,{...opts,token:''}):authenticatedRequest(route.service,path,opts);
}

function requestError(status,message){const error=new Error(message);error.status=status;return error;}
function errorMessage(data,status){
    const message=data&&typeof data==='object'?(data.message||data.error):'';
    if(status===401)return 'Authentication failed. Please sign in again.';
    if(status===403)return typeof message==='string'&&message.trim()?message:'You do not have permission to perform this action.';
    return typeof message==='string'&&message.trim()?message:`Request failed (${status}). Please try again.`;
}

async function serviceRequest(service,path,{token='',...options}={}){
    const base=window.MED_SUPPORT_CONFIG?.apiBaseUrls?.[service];
    if(typeof base!=='string'||!base.trim())throw Error('Service API URL is missing from config.js.');
    let url;
    try{url=new URL(base);}catch{throw Error('Service API URL is invalid.');}
    if(!['http:','https:'].includes(url.protocol)||url.username||url.password||url.search||url.hash){
        throw Error('Service API URL must be a public HTTP(S) base URL without credentials.');
    }
    if(!path.startsWith('/api/'))throw Error('Invalid service API path.');
    const headers={Accept:'application/json',...(options.body&&!(options.body instanceof FormData)?{'Content-Type':'application/json'}:{}),...(token?{Authorization:'Bearer '+token}:{})};
    let response;
    try{
        response=await fetch(base.replace(/\/+$/,'')+path,{
            ...options,headers,credentials:'omit',cache:'no-store',redirect:'error'
        });
    }catch{throw Error('Unable to reach '+service+' Service. Check its availability and the approved frontend CORS origin.');}
    const text=await response.text();let data;try{data=JSON.parse(text)}catch{data=null;}
    if(!response.ok)throw requestError(response.status,errorMessage(data,response.status));
    if(response.status===204)return null;
    if(!data||typeof data!=='object')throw Error(service+' Service returned an invalid response.');
    return data;
}


function identityRequest(path,options={}){
    const route=resolveApiRoute(path,options.method||'GET');
    if(route.service!=='identity')throw requestError(0,'Unsupported Identity API action.');
    if(!route.public&&!options.token)throw requestError(401,'Please sign in.');
    return serviceRequest('identity',path,{...options,token:route.public?'':options.token});
}
async function authenticatedRequest(service,path,options={}){
    const token=state.token,generation=sessionGeneration;
    if(!token)throw requestError(401,'Please sign in.');
    try{
        const data=await serviceRequest(service,path,{...options,token});
        if(token!==state.token||generation!==sessionGeneration)throw Error('Your session changed. Please try again.');
        return data;
    }catch(error){
        if(error.status===401&&token===state.token&&generation===sessionGeneration)signout();
        throw error;
    }
}
function retainDonorProfile(data){
    if(!data||!Number.isInteger(data.id)||data.id<=0||data.userId!==state.user?.id)throw Error('Invalid donor profile response.');
    state.donorProfileId=data.id;
    return data;
}
async function getDonorProfile(){
    try{return retainDonorProfile(await api('/api/donor-profile/me'));}
    catch(error){if(error.status===404){state.donorProfileId=null;return null;}throw error;}
}
function donorOnboarding(){
    return '<p class="hint">Upload your CPR document under My Documents first. If already uploaded, you may submit now; Identity checks the prerequisite.</p><form id="donorOnboarding"><div class="form-grid">'
        +select('donorBloodType','Blood type',['A+','A-','B+','B-','AB+','AB-','O+','O-'])
        +field('donorBirth','Date of birth','date','required')
        +field('donorGender','Gender','text','required')
        +field('donorWeight','Weight (kg)','number','min="0.1" step="0.1" required')
        +field('donorLastDonation','Last donation date (optional)','date')
        +field('donorConditions','Medical conditions (optional)')
        +field('donorMedications','Medications (optional)')
        +field('donorAllergies','Allergies (optional)')
        +field('donorNotes','Donor notes (optional)')
        +'</div><button class="button">Create donor profile</button></form>';
}
function donorProfilePayload(){
    const dateOfBirth=val('donorBirth'),weightKg=Number(val('donorWeight'));
    if(!/^\d{4}-\d{2}-\d{2}$/.test(dateOfBirth)||dateOfBirth>=utcToday()||!Number.isFinite(weightKg)||weightKg<=0||!val('donorGender').trim())throw Error('Enter a past date of birth, gender, and positive weight.');
    return {bloodType:val('donorBloodType'),dateOfBirth,gender:val('donorGender').trim(),weightKg,
        lastDonationDate:val('donorLastDonation')||null,medicalConditions:val('donorConditions').trim()||null,
        medications:val('donorMedications').trim()||null,allergies:val('donorAllergies').trim()||null,notesAboutTheDonor:val('donorNotes').trim()||null};
}

function validateSessionAccount(account){
    if(!account||!Number.isInteger(account.id)||account.id<=0||typeof account.email!=='string'||!account.email.trim()
            ||!['DONOR','HOSPITAL_STAFF','ADMIN'].includes(account.role)){
        throw Error('Identity Service returned invalid account details.');
    }
    if(account.status!=='ACTIVE'||account.emailVerified!==true){
        throw requestError(403,'Your account must be active and email-verified to sign in.');
    }
}

function initializeSession(token,account){
    validateSessionAccount(account);
    // Persist only server-returned session values, never form-selected roles or passwords.
    try{
        sessionStorage.setItem('med_token',token);
        sessionStorage.setItem('med_email',account.email);
        sessionStorage.setItem('med_role',account.role);
    }catch{
        throw Error('Session storage is unavailable. Enable storage for this frontend and try again.');
    }
    state.token=token;state.email=account.email;state.role=account.role;state.user=account;
    connectNotifications();
}

let accountRequestGeneration=0;
function acceptAccountResponse(account){
    validateSessionAccount(account);
    if(account.id!==state.user?.id||account.email!==state.email||account.role!==state.role)throw Error('Your account details changed. Please sign out and sign in again.');
    initializeSession(state.token,account);
    accountRequestGeneration++;
    return account;
}
async function refreshAccount(){
    const request=++accountRequestGeneration;
    const account=await api('/api/users/me');
    if(request!==accountRequestGeneration)return state.user;
    return acceptAccountResponse(account);
}
function clearSession(){
    clearTimeout(notificationReconnect); notificationReconnect=null; notificationSocketToken='';
    accountRequestGeneration++;
    sessionGeneration++;
    if(state.socket){try{state.socket.close()}catch{}state.socket=null;}
    state.token='';state.email='';state.role='GUEST';state.user=null;state.donorProfileId=null;state.bookingContext=null;
    state.hospitals=[];state.requests=[];state.bookings=[];state.notifications=[];state.slots=[];state.progress={};

    for(const key of ['med_token','med_role','med_email']){try{sessionStorage.removeItem(key)}catch{}}
    
}

async function restoreSession(){
    let token='';try{token=sessionStorage.getItem('med_token')||'';}catch{}
    if(!token){clearSession();navigate(sharedRequestId()?'shared':'auth');return;}
    const generation=++sessionGeneration;
    $('#view').textContent='Checking your saved session…';
    try{
        const account=await identityRequest('/api/users/me',{token});
        if(generation!==sessionGeneration)return;
        initializeSession(token,account);
        navigate(resumePage());
    }catch(error){
        if(generation!==sessionGeneration)return;
        clearSession();navigate(sharedRequestId()?'shared':'auth');notify(error.message,true);
    }
}
const navIcons={"overview":"M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z","hospitals":"M4 21V5h16v16M9 21v-5h6v5M12 7v6M9 10h6","requests":"M12 3S5 11 5 15a7 7 0 0 0 14 0c0-4-7-12-7-12Z","slots":"M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2ZM7 3v4m10-4v4M3 11h18","bookings":"M8 4H5v17h14V4h-3M8 3h8v4H8ZM8 13l3 3 5-5","profile":"M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2","admin":"M4 6h16M4 12h16M4 18h16M8 3v6m8 0v6M9 15v6","notifications":"M5 17h14l-2-3V9A5 5 0 0 0 7 9v5ZM10 21h4","auth":"M14 4h6v16h-6M3 12h12m-4-4 4 4-4 4"};
function navIcon(key){return `<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${navIcons[key]||navIcons.overview}"/></svg>`}
function renderNav(){const allowed=state.token?['overview','hospitals','requests',...(state.role==='ADMIN'?[]:['slots','bookings']),'notifications','departments','profile',...(state.role==='ADMIN'?['admin']:[])]:['overview','hospitals','auth'];$('#nav').innerHTML=allowed.map(k=>`<button data-nav="${k}" ${state.page===k?'aria-current="page"':''} class="${state.page===k?'active':''}">${navIcon(k)} <span>${pages[k][1]}</span></button>`).join('');$('#roleBadge').textContent=({DONOR:'Donor',HOSPITAL_STAFF:'Hospital staff',ADMIN:'Administrator',GUEST:'Guest'})[state.role]||state.role;$('#logout').style.display=state.token?'block':'none';document.querySelectorAll('[data-nav]').forEach(b=>b.onclick=()=>navigate(b.dataset.nav))}
function navigate(p){
    if(!Object.prototype.hasOwnProperty.call(pages,p)){notify('This frontend feature is not available in Project 4.',true);return;}
    const url=new URL(location.href);
    if(['requests','slots'].includes(p))url.searchParams.set('page',p);else url.searchParams.delete('page');
    history.replaceState(null,'',url.pathname+url.search+url.hash);
    profileVisit++;state.page=p;$('#notice').textContent='';renderNav();$('#pageTitle').textContent=pages[p][1];
    ({overview,hospitals,requests,slots,bookings,notifications,departments,profile,admin,auth,shared})[p]();
}
function panel(title,content,subtitle=''){return `<div class="panel"><div class="panel-head"><div><h2>${title}</h2>${subtitle?`<p class="hint">${subtitle}</p>`:''}</div></div>${content}</div>`}
function field(name,label,type='text',extra=''){return `<div class="field"><label for="${name}">${label}</label><input id="${name}" name="${name}" type="${type}" ${extra}></div>`}
function select(name,label,options){return `<div class="field"><label for="${name}">${label}</label><select id="${name}" name="${name}">${options.map(v=>`<option value="${esc(v)}">${esc(v)}</option>`).join('')}</select></div>`}
function val(id){return $('#'+id)?.value||''}
function statusPill(s){const v=String(s||'Unknown');return `<span class="pill ${/OPEN|ACTIVE|CONFIRMED|COMPLETED/i.test(v)?'green':'red'}">${esc(v)}</span>`}
let hospitalRequestGeneration=0,hospitalPageGeneration=0;
async function loadHospitals({isCurrent=()=>true}={}){
    const generation=++hospitalRequestGeneration;
    try{
        const rows=await api('/api/hospitals');
        if(!Array.isArray(rows))throw Error('Invalid hospital list response.');
        if(generation!==hospitalRequestGeneration||!isCurrent())return null;
        state.hospitals=rows;
        const connection=$('#connection');if(connection)connection.textContent='● Hospital API connected';
        return rows;
    }catch(error){
        if(generation===hospitalRequestGeneration&&isCurrent()){
            const connection=$('#connection');if(connection)connection.textContent='● Hospital API unavailable';
        }
        throw error;
    }
}
function overview(){
    const visit=profileVisit,token=state.token,role=state.role;
    const current=()=>state.page==='overview'&&profileVisit===visit&&state.token===token&&state.role===role;
    const staff=state.role==='HOSPITAL_STAFF';
    if(staff){staffDashboard();return;}
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
</svg></div></div><div class="stats"><div class="stat"><span class="icon" aria-hidden="true">✚</span><p>Partner hospitals</p><strong id="hospitalTotal">—</strong><small>Across the care network</small></div><div class="stat"><span class="icon" aria-hidden="true">♡</span><p>Active hospitals</p><strong id="activeTotal">—</strong><small>Supporting donation care</small></div></div>${panel('Your donation workspace',`<div class="grid"><div class="item"><span class="pill red">01 · BLOOD REQUESTS</span><h3>Connect a need with a donor</h3><p>${staff?'Publish blood requirements and track request status with your care team.':'Browse hospital requests and find where your donation can help.'}</p><button class="button secondary" data-quick="requests">View blood requests →</button></div><div class="item"><span class="pill red">02 · APPOINTMENTS</span><h3>A clear schedule for better care</h3><p>${staff?'Set appointment capacity and monitor how many places remain.':'Choose an available slot, review your booking and manage your appointment.'}</p><button class="button secondary" data-quick="slots">${staff?'Manage donation slots':'Find donation slots'} →</button></div></div>`)}<p class="footer-note">Hospital availability and appointment status are provided by the connected care network.</p>`;
    $('#explore').onclick=()=>navigate('requests');
    if(role==='ADMIN'){
        const action=document.querySelector('[data-quick="slots"]');
        action.dataset.quick='admin';action.textContent='Manage hospitals and accounts →';
        action.closest('.item').querySelector('h3').textContent='Hospital and account administration';
        action.closest('.item').querySelector('p').textContent='Manage hospitals, staff assignment and account access.';
    }
    document.querySelectorAll('[data-quick]').forEach(b=>b.onclick=()=>navigate(b.dataset.quick));
    loadHospitals({isCurrent:current}).then(rows=>{
        if(!rows||!current())return;
        $('#hospitalTotal').textContent=rows.length;
        $('#activeTotal').textContent=rows.filter(h=>h.hospitalActive).length;
    }).catch(error=>{if(current())notify('Hospital information is currently unavailable. '+error.message,true);});
}

async function hospitals(){
    const generation=++hospitalPageGeneration,visit=profileVisit,token=state.token,role=state.role;
    const current=()=>generation===hospitalPageGeneration&&state.page==='hospitals'&&profileVisit===visit&&state.token===token&&state.role===role;
    let records=null;
    $('#view').innerHTML='<div class="toolbar"><input aria-label="Search hospitals" id="hospitalSearch" placeholder="Search hospitals by name or location"><select id="hospitalSort" aria-label="Sort hospitals"><option value="name">Hospital name</option><option value="location">Location</option><option value="status">Active first</option></select></div><div id="hospitalList">Loading hospitals...</div>';
    const search=$('#hospitalSearch');
    search.oninput=()=>{if(current()&&records)drawHospitals(records);};
    $('#hospitalSort').onchange=()=>{if(current()&&records)drawHospitals(records);};
    try{
        const rows=await loadHospitals({isCurrent:current});
        if(!rows||!current())return;
        records=rows;drawHospitals(records);
    }catch(error){
        if(current()){$('#hospitalList').textContent='Unable to load hospitals.';notify(error.message,true);}
    }
}
function drawHospitals(rows=state.hospitals){if(!$('#hospitalSearch')||!$('#hospitalList'))return;const hs=searchSortHospitals(rows.filter(h=>state.role!=='HOSPITAL_STAFF'||h.id===state.user?.hospitalId),val('hospitalSearch'),val('hospitalSort'));$('#hospitalList').innerHTML=hs.length?`<div class="grid">${hs.map(h=>`<div class="item"><div class="split"><span class="pill red">✚ Hospital</span>${statusPill(h.hospitalActive?'ACTIVE':'INACTIVE')}</div><h3>${esc(h.hospitalName)}</h3><p>⌖ ${esc(h.hospitalLocation)}</p><p>${esc(h.hospitalDescription)}</p><div class="meta"><span class="pill">☎ ${esc(h.hospitalPhone)}</span><span class="pill">ID ${h.id}</span></div>${hospitalDirections(h)}<details><summary>Hospital details</summary>${profileFields([['Hospital',h.hospitalName],['Location',h.hospitalLocation]])}${hospitalDirections(h)}</details></div>`).join('')}</div>`:'<div class="empty">No hospitals found.</div>'}
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


function sharedRequestId(){
    const id=new URLSearchParams(location.search).get('request');
    return /^[1-9]\d*$/.test(id||'')&&positiveId(id)?id:null;
}
function requestShareUrl(id){
    if(!positiveId(id))throw Error('Invalid request ID.');
    const url=new URL(location.pathname,location.origin);
    url.searchParams.set('request',id);return url.href;
}
function shareControls(request){
    if(!requestIsAvailable(request))return '';
    const url=requestShareUrl(request.id);
    return '<details><summary>Share request</summary><div class="actions"><button class="button secondary" data-copy-request="'+esc(request.id)+'">Copy link</button><a class="button secondary" target="_blank" rel="noopener noreferrer" href="https://wa.me/?text='+encodeURIComponent('Blood donation request: '+url)+'">WhatsApp</a>'+(navigator.share?'<button class="button secondary" data-native-request="'+esc(request.id)+'">Share…</button>':'')+'</div><input aria-label="Request link" readonly value="'+esc(url)+'"></details>';
}
function wireSharing(){
    document.querySelectorAll('[data-copy-request]').forEach(button=>button.onclick=async()=>{
        const url=requestShareUrl(button.dataset.copyRequest);
        try{await navigator.clipboard.writeText(url);notify('Request link copied.');}
        catch{button.closest('details').querySelector('input').select();notify('Select and copy the request link shown below.');}
    });
    document.querySelectorAll('[data-native-request]').forEach(button=>button.onclick=async()=>{
        try{await navigator.share({title:'Blood donation request',url:requestShareUrl(button.dataset.nativeRequest)});}
        catch(error){if(error.name!=='AbortError')notify('Sharing is unavailable. Use Copy link or WhatsApp.',true);}
    });
}
function hospitalDirections(hospital){
    const name=String(hospital?.hospitalName||'').trim().toLowerCase().replace(/\s+/g,' ');
    // Exact aliases only; never infer a hospital from a substring.
    const destinations={
        'salmaniya medical complex':'Salmaniya Medical Complex, 6H8C+JVW, Road No 2904, Manama, Bahrain',
        'bahrain defence force hospital':'Bahrain Defence Force Hospital, Wadi Al Sail, West Riffa, Bahrain',
        'bdf military hospital':'Bahrain Defence Force Hospital, Wadi Al Sail, West Riffa, Bahrain',
        'bdf hospital':'Bahrain Defence Force Hospital, Wadi Al Sail, West Riffa, Bahrain'
    };
    const location=String(hospital?.hospitalLocation||'').trim().toLowerCase().replace(/\s+/g,' ');
    const destination=destinations[name]||(name==='military hospital'&&['wadi al sail, west riffa, bahrain','wadi alsail, west riffa, bahrain'].includes(location)?destinations['bdf hospital']:null);
    const supplied=String(hospital?.hospitalLocation||'').trim();
    const target=destination||(supplied?String(hospital.hospitalName||'')+', '+supplied:'');
    return target?'<p><a class="button secondary" target="_blank" rel="noopener noreferrer" href="https://www.google.com/maps/dir/?api=1&destination='+encodeURIComponent(target)+'">'+(destination?'View Location / Get Directions':'Directions to supplied location')+'</a></p>':'';
}
async function requestDirections(hospitalId){
    try{return hospitalDirections(await api('/api/hospitals/'+encodeURIComponent(hospitalId)));}catch{return '';}
}
async function shared(){
    const id=sharedRequestId(),visit=profileVisit;
    $('#view').innerHTML=panel('Shared blood request','Loading request...');
    try{
        if(!id)throw Error('Invalid request link.');
        const request=await api('/api/blood-requests/shared/'+id);
        const directions=await requestDirections(request.hospitalId);
        if(state.page!=='shared'||visit!==profileVisit)return;
        $('#view').innerHTML=panel('Blood request #'+esc(request.id),urgentBadge(request)+profileFields([
            ['Hospital',request.hospitalName],['Blood type',request.requiredBloodType],
            ['Required units',request.requiredBloodUnits],['Urgency',request.bloodRequestUrgency],
            ['Needed by',request.neededDate],['Status',request.bloodRequestStatus]])
            +directions+shareControls(request)
            +(requestIsAvailable(request)?'<button class="button" id="sharedDonate">Book / Donate</button>':'<p>This request is no longer available for booking.</p>'));
        wireSharing();
        const button=$('#sharedDonate');if(button)button.onclick=()=>{
            if(!state.token){navigate('auth');notify('Sign in or create a donor account to continue with request #'+id+'.');return;}
            if(state.role!=='DONOR'){notify('A donor account is required to book.',true);return;}
            donorBookingForm(id);
        };
    }catch(error){if(state.page==='shared'&&visit===profileVisit){$('#view').innerHTML=panel('Request unavailable','<p>This request is closed, completed, deleted, or cannot currently be loaded.</p>');notify(error.message,true);}}
}

function appointmentInstant(date,time){return new Date(date+'T'+time+'Z');}
function bahrainAppointment(date,time){
    const instant=appointmentInstant(date,time);
    return Number.isFinite(instant.getTime())?new Intl.DateTimeFormat('en-GB',{timeZone:'Asia/Bahrain',dateStyle:'medium',timeStyle:'short'}).format(instant)+' (Bahrain)':null;
}
async function loadRequestProgress(current){
    try{
        const rows=await api('/api/blood-requests/progress');
        if(!Array.isArray(rows))throw Error('Invalid progress response.');
        if(current())state.progress=Object.fromEntries(rows.map(row=>[row.requestId,row]));
    }catch(error){if(current()){state.progress={};notify('Donation progress is unavailable. '+error.message,true);}}
}
function requestProgress(request){
    const p=state.progress?.[request.id];
    if(!p||!Number.isFinite(p.completedUnits)||!Number.isFinite(p.requiredUnits)||p.requiredUnits<1)return '<p class="hint">Donation progress unavailable.</p>';
    const percent=Math.min(100,Math.max(0,Math.round(100*p.completedUnits/p.requiredUnits)));
    return '<div class="donation-progress"><label>'+esc(p.completedUnits)+' of '+esc(p.requiredUnits)+' completed donations · '+percent+'%<progress max="100" value="'+percent+'" aria-label="Donation progress"></progress></label></div>';
}
async function staffDashboard(){
    const current=managementScope();
    $('#view').innerHTML=panel('Hospital dashboard','Loading hospital statistics...');
    try{
        const account=await verifiedStaff();
        const [requests,counts,hospital,bookings]=await Promise.all([api('/api/blood-requests/hospital'),api('/api/donation-bookings/hospital-summary'),api('/api/hospitals/'+account.hospitalId),api('/api/donation-bookings/my-hospital')]);
        if(!current())return;
        if(!Array.isArray(requests)||requests.some(r=>r.hospitalId!==account.hospitalId))throw Error('Unexpected hospital response.');
        $('#view').innerHTML=panel(esc(hospital.hospitalName)+' · Dashboard','<div class="stats">'+[
            ['Open blood requests',requests.filter(r=>r.bloodRequestStatus==='OPEN').length],
            ['Pending bookings',counts.pendingBookings],['Confirmed bookings',counts.confirmedBookings],
            ['Completed donations',counts.completedDonations]
        ].map(([label,count])=>'<div class="stat"><p>'+esc(label)+'</p><strong>'+esc(count)+'</strong></div>').join('')+'</div><div class="actions"><button class="button" data-dashboard="requests">Blood requests</button><button class="button secondary" data-dashboard="slots">Donation slots</button><button class="button secondary" data-dashboard="bookings">Donation bookings</button><button class="button secondary" id="refreshDashboard">Refresh statistics</button></div><p class="hint">Statistics cover your assigned hospital. Refresh after another user makes changes.</p>');
        document.querySelectorAll('[data-dashboard]').forEach(button=>button.onclick=()=>navigate(button.dataset.dashboard));
        $('#refreshDashboard').onclick=staffDashboard;
        if(!Array.isArray(bookings)||bookings.some(b=>b.hospitalId!==account.hospitalId))throw Error('Unexpected hospital bookings.');
        $('#view').insertAdjacentHTML('beforeend',recentBookingActivity(bookings));
    }catch(error){if(current()){$('#view').innerHTML=panel('Hospital dashboard','<p class="error">'+esc(error.message)+'</p><button class="button secondary" id="retryDashboard">Try again</button>');$('#retryDashboard').onclick=staffDashboard;}}
}
function donorHistory(rows){
    const completed=newestDonorBookings(rows.filter(b=>b.donationBookingStatus==='COMPLETED'));
    return panel('Donation history',completed.length?'<div class="grid">'+completed.map(b=>{
        const hospital=state.hospitals.find(h=>h.id===b.hospitalId);
        return '<article class="item"><h3>Thank you for donating</h3><p>Your completed donation is recorded below.</p>'+profileFields([
            ['Hospital',hospital?.hospitalName||('Hospital #'+(b.hospitalId??'unavailable'))],
            ['Donation date (UTC)',b.donationBookingDate],['Bahrain appointment',bahrainAppointment(b.donationBookingDate,b.donationBookingTime)],['Status',b.donationBookingStatus]
        ])+requestProgress({id:b.bloodRequestId})+'</article>';
    }).join('')+'</div>':'<div class="empty">No completed donations yet. Completed appointments will appear here.</div>');
}

function donorRequestFacts(r){
    return profileFields([['Request ID',r.id],['Hospital ID',r.hospitalId],
        ...(r.hospitalName?[['Hospital',r.hospitalName]]:[]),
        ['Required blood type',r.requiredBloodType],['Units needed',r.requiredBloodUnits],
        ['Urgency',r.bloodRequestUrgency],['Needed by',r.neededDate],
        ['Status',r.bloodRequestStatus],...('bloodRequestDescription' in r?[['Description',r.bloodRequestDescription]]:[])])+requestProgress(r);
}

let donorBookingGeneration=0;
let donorBookingMutation=false;
function positiveId(value){return Number.isSafeInteger(Number(value))&&Number(value)>0;}
async function requireDonorProfile(){
    if(state.role!=='DONOR'||!state.token)throw requestError(401,'Sign in as a donor.');
    const profile=await getDonorProfile();
    if(!profile)throw Error('Complete your donor profile and CPR upload in My Profile before booking.');
    return profile;
}
function bookingSlotAvailable(slot,request,now=new Date()){
    if(!slot||!request)return false;
    const start=appointmentInstant(slot.slotDate,slot.startTime),end=appointmentInstant(slot.slotDate,slot.endTime);
    return positiveId(slot.id)&&slot.status==='OPEN'&&slot.hospitalId===request.hospitalId
        &&Number.isInteger(slot.capacity)&&Number.isInteger(slot.reservedCount)
        &&slot.reservedCount>=0&&slot.reservedCount<slot.capacity
        &&slot.slotDate<=request.neededDate&&Number.isFinite(start.getTime())
        &&Number.isFinite(end.getTime())&&start<end&&end>now;
}
function bookingPayload(profile,request,slot,time){
    if(!positiveId(profile?.id)||!positiveId(request?.id)||!bookingSlotAvailable(slot,request))throw Error('The selected slot is unavailable. Refresh availability.');
    const chosen=appointmentInstant(slot.slotDate,time),start=appointmentInstant(slot.slotDate,slot.startTime),end=appointmentInstant(slot.slotDate,slot.endTime);
    if(!/^\d{2}:\d{2}(:\d{2})?$/.test(time)||!Number.isFinite(chosen.getTime())||chosen<=new Date()||chosen<start||chosen>=end)throw Error('Choose a future booking time within the selected slot (before its end time).');
    return {donorProfileId:profile.id,bloodRequestId:request.id,donationSlotId:slot.id,donationBookingDate:slot.slotDate,donationBookingTime:time};
}
async function refreshDonorAvailability(requestId){
    const rows=await api('/api/donation-slots/available?bloodRequestId='+encodeURIComponent(requestId));
    if(!Array.isArray(rows))throw Error('Invalid slot response.');
    state.slots=rows;return rows;
}
async function donorBookingForm(requestId,selectedSlotId,rescheduling=null){
    
    if(!positiveId(requestId)){notify('Choose a valid blood request.',true);return;}
    const generation=++donorBookingGeneration,token=state.token,page=state.page,visit=profileVisit;
    const current=()=>generation===donorBookingGeneration&&state.token===token&&state.page===page&&profileVisit===visit&&!!$('#donorSlotForm');
    const previous=state.bookingContext,sameRequest=previous?.requestId===Number(requestId);
    const selected=selectedSlotId??(sameRequest?previous.slotId:null);
    const previousTime=sameRequest&&String(previous.slotId)===String(selected)?previous.time:'';
    state.bookingContext={requestId:Number(requestId),slotId:selected?Number(selected):null,time:previousTime||''};
    $('#view').innerHTML=panel('Choose your donation appointment','<div id="donorSlotForm">Loading available slots...</div>');
    let profile,request,choices=[];
    try{
        profile=await requireDonorProfile();
        request=await loadBookableRequest(requestId);
        const available=await refreshDonorAvailability(requestId);
        await loadRequestProgress(current);
        if(!current())return;
        choices=available.filter(slot=>bookingSlotAvailable(slot,request)&&(!rescheduling||slot.id!==rescheduling.donationSlotId));
        draw();
    }catch(error){
        if(current()){
            $('#donorSlotForm').innerHTML='<p class="hint">'+esc(error.message)+'</p><button class="button secondary" id="donorBookingProfile">My Profile</button><button class="button secondary" id="donorBookingBack">Back to requests</button>';
            $('#donorBookingProfile').onclick=()=>navigate('profile');$('#donorBookingBack').onclick=requests;
        }
    }
    function draw(){
        if(!current())return;
        $('#donorSlotForm').innerHTML=donorRequestFacts(request)
            +(choices.length?'<form id="donorBookForm"><div class="field"><label for="donorSlotChoice">Available donation slot</label><select id="donorSlotChoice" required>'+choices.map(slot=>'<option value="'+esc(slot.id)+'" '+(String(slot.id)===String(state.bookingContext.slotId)?'selected':'')+'>'+esc(slot.slotDate)+' · '+esc(slot.startTime)+'–'+esc(slot.endTime)+' · '+(slot.capacity-slot.reservedCount)+' place(s) remaining</option>').join('')+'</select></div>'+field('donorBookingTime','Booking time (UTC)','time','step="1" required')+'<p class="hint">Enter the slot time in UTC (Bahrain is UTC+3). Capacity and eligibility are checked by the backend when booking.</p><button class="button" id="donorBookSubmit">Confirm appointment</button></form>':'<div class="empty">No eligible slots are currently available for this request.</div>')
            +'<div class="actions"><button class="button secondary" id="donorSlotRefresh">Refresh availability</button><button class="button secondary" id="donorBookingBack">Back to requests</button></div>';
        $('#donorBookingBack').onclick=requests;
        $('#donorSlotRefresh').onclick=()=>{if(!donorBookingMutation)return donorBookingForm(requestId,state.bookingContext.slotId);};
        if(!choices.length)return;
        const form=$('#donorBookForm'),select=$('#donorSlotChoice');
        if(rescheduling){
            $('#donorBookSubmit').textContent='Save new appointment';
            form.insertAdjacentHTML('afterbegin','<p class="hint">Reschedule booking #'+esc(rescheduling.id)+'. Its ID is retained and the new appointment requires staff confirmation. Rescheduling is unavailable once a reminder has been queued. Your original reservation is retained if this change fails.</p>');
        }
        function selectSlot(){
            const slot=choices.find(s=>String(s.id)===select.value);
            if(!slot)return;
            const context=state.bookingContext,same=String(context.slotId)===String(slot.id);
            context.slotId=slot.id;
            $('#donorBookingTime').min=slot.startTime;$('#donorBookingTime').max=slot.endTime;
            let time=slot.startTime;
            if(same&&context.time){
                try{bookingPayload(profile,request,slot,context.time);time=context.time;}catch{}
            }
            $('#donorBookingTime').value=time;
            context.time=$('#donorBookingTime').value;
        }
        select.onchange=selectSlot;selectSlot();
        $('#donorBookingTime').oninput=()=>{state.bookingContext.time=val('donorBookingTime');};
        form.onsubmit=async event=>{
            event.preventDefault();if(donorBookingMutation)return;
            const button=$('#donorBookSubmit');button.disabled=true;donorBookingMutation=true;
            let created=false;
            try{
                const slot=choices.find(s=>String(s.id)===val('donorSlotChoice'));
                const payload=bookingPayload(profile,request,slot,val('donorBookingTime'));
                await api(rescheduling?'/api/donation-bookings/'+rescheduling.id+'/reschedule':'/api/donation-bookings',{method:rescheduling?'PUT':'POST',body:JSON.stringify(payload)});
                created=true;
                // Invalidate cached capacity as soon as the mutation succeeds.
                state.slots=[];state.bookings=[];
                let refreshWarning='';
                try{await refreshDonorAvailability(requestId);}catch{refreshWarning=' Availability could not be refreshed; refresh before another booking.';}
                if(current()){
                    state.page='bookings';renderNav();$('#pageTitle').textContent='My Donation Appointments';
                    await donorBookings();
                    if(state.token===token&&state.page==='bookings'&&profileVisit===visit)notify((rescheduling?'Appointment rescheduled; hospital confirmation is required.':'Your donation appointment was booked successfully.')+refreshWarning);
                }
            }catch(error){
                if(current()){
                    notify(error.message+(error.status?'':' If the request reached the service, check My Donations before trying again.'),true);
                    // A conflict may reflect changed capacity. Never automatically retry POST.
                    if(error.status===409||error.status===400){
                        try{const rows=await refreshDonorAvailability(requestId);if(current()){choices=rows.filter(slot=>bookingSlotAvailable(slot,request)&&(!rescheduling||slot.id!==rescheduling.donationSlotId));draw();}}catch{}
                    }
                }
            }finally{donorBookingMutation=false;if(!created&&current()&&button.isConnected)button.disabled=false;}
        };
    }
}
function donorBookingCards(rows){
    return rows.length?'<div class="grid">'+newestDonorBookings(rows).map(b=>'<article class="item appointment-card"><div class="appointment-card-head"><span class="appointment-reference">Booking #'+esc(b.id)+'</span>'+statusPill(b.donationBookingStatus)+'</div><h3>Donation appointment</h3>'+profileFields([['Hospital',state.hospitals.find(h=>h.id===b.hospitalId)?.hospitalName||('Hospital #'+b.hospitalId)],['Blood request ID',b.bloodRequestId],['Slot ID',b.donationSlotId],['Appointment date',b.donationBookingDate],['Appointment time (UTC)',b.donationBookingTime],['Bahrain appointment',bahrainAppointment(b.donationBookingDate,b.donationBookingTime)],['Created',b.createdAt],['Updated',b.updatedAt]])+hospitalDirections(state.hospitals.find(h=>h.id===b.hospitalId))+(['PENDING','CONFIRMED'].includes(b.donationBookingStatus)?'<button class="button secondary" data-donor-cancel="'+esc(b.id)+'">Cancel booking</button>':'')+(upcomingBooking(b)?'<button class="button secondary" data-donor-reschedule="'+esc(b.id)+'">Reschedule</button>':'')+'</article>').join('')+'</div>':'<div class="empty">No donation bookings yet.</div>';
}
async function donorBookings(){
    
    const generation=++donorBookingGeneration,token=state.token,page=state.page,visit=profileVisit;
    const current=()=>generation===donorBookingGeneration&&token===state.token&&state.page===page&&profileVisit===visit&&!!$('#donorMyBookings');
    $('#view').innerHTML=panel('My Donation Appointments','<p class="hint">An automatic reminder is queued once for a pending or confirmed appointment within the next 24 hours. View it in Notifications. Stored times are UTC; Bahrain time is shown alongside.</p><div id="donorMyBookings">Loading your bookings...</div>');
    try{
        const profile=await requireDonorProfile();
        const [rows,hospitals]=await Promise.all([api('/api/donation-bookings/my'),api('/api/hospitals'),loadRequestProgress(current)]);
        if(!current())return;
        if(!Array.isArray(rows)||rows.some(b=>b.donorProfileId!==profile.id))throw Error('Invalid bookings response for the signed-in donor.');
        state.bookings=rows;
        state.hospitals=hospitals;
        const upcoming=rows.filter(upcomingBooking).sort((a,b)=>appointmentInstant(a.donationBookingDate,a.donationBookingTime)-appointmentInstant(b.donationBookingDate,b.donationBookingTime));
        const other=rows.filter(b=>b.donationBookingStatus!=='COMPLETED'&&!upcomingBooking(b));
        $('#donorMyBookings').innerHTML=panel('Upcoming donations',upcoming.length?donorBookingCards(upcoming):'<div class="empty">No upcoming donations.</div>')+(other.length?'<details><summary>Other appointments and cancellations ('+other.length+')</summary>'+donorBookingCards(other)+'</details>':'')+donorHistory(rows)+'<div class="actions"><button class="button secondary" id="refreshDonorBookings">Refresh bookings</button><button class="button" id="findDonorRequest">Find a blood request</button></div>';
        $('#refreshDonorBookings').onclick=donorBookings;$('#findDonorRequest').onclick=()=>navigate('requests');
        document.querySelectorAll('[data-donor-reschedule]').forEach(button=>button.onclick=()=>{
            const booking=rows.find(b=>String(b.id)===button.dataset.donorReschedule);
            if(booking&&upcomingBooking(booking)&&!donorBookingMutation)donorBookingForm(booking.bloodRequestId,null,booking);
        });
        document.querySelectorAll('[data-donor-cancel]').forEach(button=>button.onclick=async()=>{
            if(donorBookingMutation)return;
            const booking=rows.find(b=>String(b.id)===button.dataset.donorCancel);
            if(!booking||!confirm('Cancel booking #'+booking.id+'?'))return;
            donorBookingMutation=true;button.disabled=true;
            let cancelled=false;
            try{
                await api('/api/donation-bookings/'+encodeURIComponent(booking.id),{method:'DELETE'});
                cancelled=true;state.bookings=[];state.slots=[];
                let warning='';
                try{await refreshDonorAvailability(booking.bloodRequestId);}catch{warning=' Slot availability could not be refreshed.';}
                if(current()){await donorBookings();if(state.token===token&&state.page===page&&profileVisit===visit)notify('Booking cancelled.'+warning);}
            }catch(error){if(current())notify(error.message+(error.status?'':' Check My Donations before trying again.'),true);}
            finally{donorBookingMutation=false;if(!cancelled&&current()&&button.isConnected)button.disabled=false;}
        });
    }catch(error){
        if(current()){$('#donorMyBookings').innerHTML='<p class="hint">'+esc(error.message)+'</p><button class="button secondary" id="donorBookingsProfile">My Profile</button><button class="button secondary" id="refreshDonorBookings">Try again</button>';$('#donorBookingsProfile').onclick=()=>navigate('profile');$('#refreshDonorBookings').onclick=donorBookings;}
    }
}

let donorRequestGeneration=0;
function isUrgentRequest(request){return requestLifecycle(request)==='active'&&['HIGH','CRITICAL'].includes(request.bloodRequestUrgency);}
function urgentBadge(request){return isUrgentRequest(request)?'<span class="pill red urgent-badge">'+esc(request.bloodRequestUrgency)+' · Urgent</span>':'';}
function requestHospital(request){return request.hospitalName||state.hospitals.find(h=>h.id===request.hospitalId)?.hospitalName||('Hospital #'+request.hospitalId);}
function requestListTools(prefix){return '<div class="toolbar"><input id="'+prefix+'Search" aria-label="Search blood requests" placeholder="Search request ID, hospital, blood type or urgency"><select id="'+prefix+'Sort" aria-label="Sort blood requests"><option value="urgency">Urgency first</option><option value="date">Needed date</option><option value="hospital">Hospital</option></select></div>';}
function searchSortRequests(rows,search='',sort='urgency'){
    const q=search.trim().toLowerCase(),rank={CRITICAL:4,HIGH:3,MEDIUM:2,LOW:1};
    return rows.filter(r=>[r.id,requestHospital(r),r.requiredBloodType,r.bloodRequestUrgency,r.bloodRequestStatus,r.bloodRequestDescription||''].join(' ').toLowerCase().includes(q)).slice().sort((a,b)=>{
        const order=sort==='hospital'?requestHospital(a).localeCompare(requestHospital(b),undefined,{numeric:true,sensitivity:'base'}):sort==='urgency'?(rank[b.bloodRequestUrgency]||0)-(rank[a.bloodRequestUrgency]||0):0;
        return order||String(a.neededDate).localeCompare(String(b.neededDate))||a.id-b.id;
    });
}
function searchSortHospitals(rows,search='',sort='name'){
    const q=search.trim().toLowerCase();
    return rows.filter(h=>[h.id,h.hospitalName,h.hospitalLocation].join(' ').toLowerCase().includes(q)).slice().sort((a,b)=>
        (sort==='status'?Number(b.hospitalActive)-Number(a.hospitalActive):sort==='location'?String(a.hospitalLocation).localeCompare(String(b.hospitalLocation)):0)
        ||String(a.hospitalName).localeCompare(String(b.hospitalName),undefined,{sensitivity:'base'})||a.id-b.id);
}
function upcomingBooking(booking){return ['PENDING','CONFIRMED'].includes(booking.donationBookingStatus)&&appointmentInstant(booking.donationBookingDate,booking.donationBookingTime)>new Date();}
function bahrainDate(date,time){
    const parts=new Intl.DateTimeFormat('en',{timeZone:'Asia/Bahrain',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(appointmentInstant(date,time));
    const values=Object.fromEntries(parts.map(part=>[part.type,part.value]));return values.year+'-'+values.month+'-'+values.day;
}
function recentBookingActivity(rows){
    const timestamp=b=>Date.parse((b.updatedAt||b.createdAt||'')+'Z');
    const latest=rows.filter(b=>Number.isFinite(timestamp(b))).slice().sort((a,b)=>timestamp(b)-timestamp(a)).slice(0,5);
    return panel('Recent activity', '<p class="hint">Latest booking states for your hospital; this is not a full audit trail.</p>'+(latest.length?latest.map(b=>'<div class="item"><strong>Booking #'+esc(b.id)+' · '+esc(b.donationBookingStatus)+'</strong><p>Request #'+esc(b.bloodRequestId)+' · '+esc(bahrainAppointment((b.updatedAt||b.createdAt).slice(0,10),(b.updatedAt||b.createdAt).slice(11)))+'</p></div>').join(''):'<div class="empty">No booking activity yet.</div>'));
}

function requestLifecycle(request,today=utcToday()){
    if(request.bloodRequestStatus==='COMPLETED')return 'completed';
    if(/^\d{4}-\d{2}-\d{2}$/.test(request.neededDate||'')){
        if(request.neededDate<today)return 'expired';
        if(request.bloodRequestStatus==='OPEN')return 'active';
    }
    return 'other';
}
function slotLifecycle(slot,now=new Date()){
    const start=appointmentInstant(slot.slotDate,slot.startTime),end=appointmentInstant(slot.slotDate,slot.endTime);
    if(!Number.isFinite(start.getTime())||!Number.isFinite(end.getTime()))return 'other';
    if(end<=now){
        if(!Number.isFinite(slot.nonCancelledBookings)||!Number.isFinite(slot.completedBookings))return 'other';
        return slot.nonCancelledBookings>0&&slot.completedBookings===slot.nonCancelledBookings?'completed':'expired';
    }
    return slot.status==='OPEN'&&start>now?'active':'other';
}
function selectedLifecycle(page,value){
    const key='med_lifecycle_'+state.role+'_'+(state.user?.id??'guest')+'_'+page;
    try{
        if(value)sessionStorage.setItem(key,value);
        const saved=sessionStorage.getItem(key);
        return ['active','completed','expired'].includes(saved)?saved:'active';
    }catch{return value||'active';}
}
function lifecycleTabs(page,rows,classify){
    const selected=selectedLifecycle(page);
    return '<div class="record-tabs" role="tablist" aria-label="Lifecycle">'+['active','completed','expired'].map(tab=>'<button type="button" role="tab" class="record-tab '+(tab===selected?'active':'')+'" aria-selected="'+(tab===selected)+'" data-life-page="'+page+'" data-life-tab="'+tab+'">'+tab[0].toUpperCase()+tab.slice(1)+' ('+rows.filter(row=>classify(row)===tab).length+')</button>').join('')+'</div>';
}
function wireLifecycleTabs(page,render){
    document.querySelectorAll('[data-life-page="'+page+'"]').forEach(button=>button.onclick=()=>{selectedLifecycle(page,button.dataset.lifeTab);render();});
}
function slotFacts(slot){
    const available=slot.status==='OPEN'&&slot.reservedCount<slot.capacity;
    const lifecycle=slotLifecycle(slot);
    return '<p>'+statusPill(lifecycle.toUpperCase())+' '+(lifecycle==='active'?statusPill(available?'AVAILABLE':'FULL'):'')+' <span class="hint">Stored status: '+esc(slot.status)+'</span></p>'+profileFields([
        ['Date (UTC)',slot.slotDate],['Start (UTC)',slot.startTime],['End (UTC)',slot.endTime],
        ['Bahrain start',bahrainAppointment(slot.slotDate,slot.startTime)],['Capacity',slot.capacity],['Reserved',slot.reservedCount],
        ['Completed',slot.completedBookings],['Cancelled',slot.cancelledBookings],['Pending',slot.pendingBookings],['Confirmed',slot.confirmedBookings]
    ]);
}
function resumePage(){
    const page=new URLSearchParams(location.search).get('page');
    return ['requests','slots'].includes(page)?page:sharedRequestId()?'shared':'overview';
}

function filterDonorRequests(rows,{hospital='',urgency='',date='',available=false}={}){
    return rows.filter(r=>(!hospital||String(r.hospitalId)===hospital)
        &&(!urgency||r.bloodRequestUrgency===urgency)&&(!date||r.neededDate===date)
        &&(!available||(requestIsAvailable(r)&&(state.progress?.[r.id]?.availableSlots||0)>0)));
}
async function donorRequests(){
    const generation=++donorRequestGeneration,token=state.token;
    const current=()=>generation===donorRequestGeneration&&state.page==='requests'&&state.token===token;
    $('#view').innerHTML=panel('Blood requests',requestListTools('donorRequest')+'<div class="form-grid"><div class="field"><label for="donorBloodFilter">Blood type</label><select id="donorBloodFilter"><option value="">All blood types</option>'+['A+','A-','B+','B-','AB+','AB-','O+','O-'].map(t=>'<option>'+esc(t)+'</option>').join('')+'</select></div><div class="field"><label for="donorHospitalFilter">Hospital</label><select id="donorHospitalFilter"><option value="">All hospitals</option></select></div><div class="field"><label for="donorUrgencyFilter">Urgency</label><select id="donorUrgencyFilter"><option value="">All urgency levels</option>'+['LOW','MEDIUM','HIGH','CRITICAL'].map(t=>'<option>'+t+'</option>').join('')+'</select></div>'+field('donorNeededFilter','Needed date (UTC)','date')+'</div><label><input id="donorAvailableFilter" type="checkbox"> Active requests with an available slot</label><button class="button secondary" id="refreshDonorRequests">Refresh requests</button><p class="hint">Counts follow your filters. Availability applies only to Active requests and is checked again when booking.</p><div id="donorRequestList">Loading requests...</div>');
    let selection=0,records=[];
    function cards(rows){return rows.length?'<div class="grid">'+rows.map(r=>'<article class="item" data-urgent="'+isUrgentRequest(r)+'"><h3>Blood request #'+esc(r.id)+'</h3>'+urgentBadge(r)+statusPill(requestLifecycle(r).toUpperCase())+donorRequestFacts(r)+shareControls(r)+(requestLifecycle(r)==='active'?'<button class="button secondary" data-donor-detail="'+esc(r.id)+'">View details</button>':'')+'</article>').join('')+'</div>':'<div class="empty">No requests match this tab and these filters.</div>';}
    function render(){
        if(!current())return;
        const selected=selectedLifecycle('donorRequests'),type=val('donorBloodFilter');
        let rows=filterDonorRequests(records,{hospital:val('donorHospitalFilter'),urgency:val('donorUrgencyFilter'),date:val('donorNeededFilter')})
            .filter(r=>(!type||r.requiredBloodType===type)&&(requestLifecycle(r)!=='active'||!$('#donorAvailableFilter').checked||(state.progress?.[r.id]?.availableSlots||0)>0));
        $('#donorAvailableFilter').disabled=selected!=='active';
        rows=searchSortRequests(rows,val('donorRequestSearch'),val('donorRequestSort'));
        const other=rows.filter(r=>requestLifecycle(r)==='other');
        $('#donorRequestList').innerHTML=lifecycleTabs('donorRequests',rows,requestLifecycle)+cards(rows.filter(r=>requestLifecycle(r)===selected))
            +'<details><summary>Closed / Other ('+other.length+')</summary>'+cards(other)+'</details>';
        wireLifecycleTabs('donorRequests',render);wireSharing();
        document.querySelectorAll('[data-donor-detail]').forEach(button=>button.onclick=()=>donorRequestDetails(button.dataset.donorDetail));
    }
    async function load(){
        const chosen=++selection;
        $('#donorRequestList').textContent='Loading requests...';
        try{
            const [rows,hospitals]=await Promise.all([api('/api/blood-requests/browse'),loadHospitals({isCurrent:current}),loadRequestProgress(()=>current()&&chosen===selection)]);
            if(!current()||chosen!==selection)return;
            if(!Array.isArray(rows))throw Error('Invalid blood request list.');
            records=rows;state.requests=rows;
            const selected=val('donorHospitalFilter');
            $('#donorHospitalFilter').innerHTML='<option value="">All hospitals</option>'+(hospitals||[]).map(h=>'<option value="'+esc(h.id)+'">'+esc(h.hospitalName)+'</option>').join('');
            $('#donorHospitalFilter').value=selected;render();
        }catch(error){if(current()&&chosen===selection){$('#donorRequestList').textContent='Unable to load requests. Use Refresh requests to try again.';notify(error.message,true);}}
    }
    for(const id of ['donorBloodFilter','donorHospitalFilter','donorUrgencyFilter','donorNeededFilter','donorAvailableFilter'])$('#'+id).onchange=render;
    $('#donorRequestSearch').oninput=render;$('#donorRequestSort').onchange=render;
    $('#refreshDonorRequests').onclick=load;await load();
}
async function donorRequestDetails(id){
    const generation=++donorRequestGeneration,token=state.token;
    const current=()=>generation===donorRequestGeneration&&state.page==='requests'&&state.token===token;
    $('#view').innerHTML=panel('Blood request','Loading request details...');
    try{
        const request=await api('/api/blood-requests/open/'+encodeURIComponent(id));
        if(!current())return;
        await loadRequestProgress(current);
        const directions=await requestDirections(request.hospitalId);
        if(!current())return;
        $('#view').innerHTML=panel('Blood request #'+esc(request.id),urgentBadge(request)+donorRequestFacts(request)+shareControls(request)+directions+'<p class="hint">Request details are provided by the service. Booking availability is confirmed when you choose a slot.</p><button class="button" id="chooseDonorSlot">Choose donation slot</button><button class="button secondary" id="backDonorRequests">Back to requests</button>');
        wireSharing();
        $('#backDonorRequests').onclick=donorRequests;
        $('#chooseDonorSlot').onclick=()=>donorBookingForm(request.id);
    }catch(error){if(current()){$('#view').innerHTML=panel('Blood request unavailable','<button class="button secondary" id="backDonorRequests">Back to requests</button>');$('#backDonorRequests').onclick=donorRequests;notify(error.message,true);}}
}

async function requests(){
    if(state.role==='DONOR')return donorRequests();
    if(['HOSPITAL_STAFF','ADMIN'].includes(state.role))return managementRequests();
    navigate('auth');
}
// BloodRequest API fields: bloodNeededDate (LocalDate), bloodRequestStatus.
function requestIsAvailable(request){
    const date=request?.neededDate??request?.bloodNeededDate;
    return request?.bloodRequestStatus==='OPEN' && typeof date==='string'
        && /^\d{4}-\d{2}-\d{2}$/.test(date) && date>=utcToday();
}
async function loadBookableRequest(id){
    const request=await api('/api/blood-requests/open/'+encodeURIComponent(id));
    if(!requestIsAvailable(request))throw Error('This blood request is no longer available. Please choose an active request.');
    return request;
}
function utcToday(){return new Date().toISOString().slice(0,10);}
async function slots(){
    if(state.role==='HOSPITAL_STAFF')return staffSlots();
    if(state.role==='DONOR')return donorSlots();
    if(state.role==='ADMIN'){$('#view').innerHTML=panel('Donation slots','<p class="hint">Hospital staff manage their assigned hospital slots.</p>');return;}
    navigate('auth');
}
async function bookings(){
    if(state.role==='DONOR')return donorBookings();
    if(state.role==='HOSPITAL_STAFF')return staffBookingWorkflow();
    if(state.role==='ADMIN'){$('#view').innerHTML=panel('Bookings','<p class="hint">Hospital staff review their own hospital bookings.</p>');return;}
    navigate('auth');
}
function bookingTransitions(status){return ({PENDING:['CONFIRMED','CANCELLED'],CONFIRMED:['COMPLETED','CANCELLED']})[status]||[];}
function detailValue(value){
    if(value===null||value===undefined||String(value).trim()==='')return '<span class="unavailable">Not available</span>';
    return esc(value);
}

function newestDonorBookings(data){
    const createdAt=booking=>Date.parse(booking.createdAt||booking.createdAtDonationBooking||'');
    // Use one consistent ordering for the whole list; partial timestamps fall back to IDs.
    const hasCreationDates=data.every(booking=>Number.isFinite(createdAt(booking)));
    return data.slice().sort((a,b)=>(hasCreationDates?createdAt(b)-createdAt(a):0)||(Number(b.id)-Number(a.id)));
}

function sessionProfileEmail(){
    if(state.email)return state.email;
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
    
    const visit=++profileVisit,token=state.token,role=state.role,email=sessionProfileEmail();
    const current=()=>visit===profileVisit&&state.token===token&&state.role===role&&!!$('#profileOverview');
    let account=state.user||{},donor={},hospital={},notice='',donorMissing=false;
    const workspace=({DONOR:'Donor',HOSPITAL_STAFF:'Hospital staff',ADMIN:'Administrator'})[role]||'Not available';
    $('#view').innerHTML='<div class="section-intro profile-intro"><div><span class="eyebrow">YOUR ACCOUNT & CARE INFORMATION</span><h2>My Profile</h2><p>Review your available account details and manage your personal settings.</p></div><button class="button secondary" type="button" id="refreshProfile">Refresh information</button></div><section class="panel profile-overview" id="profileOverview" aria-label="Profile Overview" aria-busy="true"><p class="muted">Loading your account information…</p></section><div class="profile-layout"><div class="profile-information"><section class="panel"><div class="panel-head"><h2>Personal Information</h2></div><div id="profilePersonal"><p class="muted">Loading available details…</p></div><details class="profile-edit" open><summary>Edit name and phone number</summary><form id="profileForm"><div class="form-grid">'+field('profileName','Full name','text','autocomplete="name" required')+field('profilePhone','Phone number','tel','autocomplete="tel" required')+'</div><button class="button" type="submit">Save changes</button></form></details></section><section class="panel" id="profileRoleSection"><div class="panel-head"><h2>'+ (role==='DONOR'?'Donor Information':role==='HOSPITAL_STAFF'?'Staff Information':'Administrator Information')+'</h2></div><div id="profileRoleInformation"><p class="muted">Loading available information…</p></div></section></div><section class="panel profile-settings"><div class="panel-head"><h2>Account Settings</h2></div><section class="profile-setting-section"><h3>Change password</h3><p class="hint">Use your current password to set a new password.</p><form id="passwordForm">'+field('oldPassword','Current password','password','autocomplete="current-password" required')+field('newPassword','New password','password','autocomplete="new-password" required')+'<button class="button" type="submit">Change password</button></form></section>'+(role==='DONOR'?'<section class="profile-setting-section" aria-labelledby="myDocumentsTitle"><h3 id="myDocumentsTitle">My Documents</h3><div class="profile-data-note" id="profileCprStatus" role="status" aria-live="polite"><strong>Existing CPR document: Not available</strong><br>The current service supports uploading CPR documents but does not provide authorized viewing or downloading. This does not indicate whether a document is already stored.</div><form id="cprForm"><label for="profileCprFile">CPR document · PDF, JPEG or PNG · up to 5 MB</label><input id="profileCprFile" type="file" name="file" accept=".pdf,.jpg,.jpeg,.png" required><button class="button secondary" type="submit">Upload CPR document</button></form></section>':'')+'<section class="profile-setting-section" aria-labelledby="profilePhotoTitle"><h3 id="profilePhotoTitle">Profile Photo</h3><div class="profile-data-note" id="profilePhotoStatus" role="status" aria-live="polite"><strong>Existing profile picture: Not available</strong><br>The current service does not provide an authorized image retrieval endpoint. Your initials or the medical symbol are shown as an account icon, not an uploaded photo.</div><form id="imageForm"><label for="profileImageFile">Profile photo · JPEG or PNG · up to 5 MB</label><input id="profileImageFile" type="file" name="file" accept=".jpg,.jpeg,.png" required><button class="button secondary" type="submit">Upload profile photo</button></form></section><section class="profile-setting-section profile-deactivation"><h3>Deactivate account</h3><p class="hint">Deactivation disables your account and your ability to sign in.</p><button class="button outline" type="button" id="deactivateMe">Deactivate my account</button></section></section></div>';
    $('#refreshProfile').onclick=profile;
    function drawInformation(){
        if(!current())return;
        $('#profileOverview').setAttribute('aria-busy','false');
        $('#profileOverview').innerHTML='<div class="profile-identity"><div class="profile-avatar" aria-hidden="true">'+esc(account.name?Array.from(account.name.trim())[0].toUpperCase():'✚')+'</div><div><span class="eyebrow">PROFILE OVERVIEW</span><h2>'+esc(account.name||'Name not available')+'</h2><p>'+detailValue(account.email||email)+'</p></div><span class="pill red">Workspace: '+esc(workspace)+'</span></div>'+profileFields([['Full name',account.name],['Email',account.email||email],['Phone number',account.phoneNumber],['Account role',account.role],['Account status',account.status],['Email verified',profileBoolean(account.emailVerified)]])+'<div class="profile-data-note" role="status">'+esc(notice)+'</div>';
        $('#profilePersonal').innerHTML=profileFields([['Full name',account.name],['Email',account.email||email],['Phone number',account.phoneNumber],['Account ID',account.id],['Account created',account.createdAtUser],['Last account update',account.updatedAtUser]]);
        if(role==='DONOR'){
            const availability=typeof donor.availableForBloodDonation==='boolean'?donor.availableForBloodDonation:donor.isAvailableForBloodDonation;
            $('#profileRoleInformation').innerHTML=profileFields([['Donor profile ID',donor.id],['Blood type',donor.bloodType],['Date of birth',donor.dateOfBirth],['Gender',donor.gender],['Weight (kg)',donor.weightKg],['Donation availability',profileBoolean(availability,'Available','Unavailable')],['Last donation date',donor.lastDonationDate],['Total donations',donor.totalDonations],['Medical conditions',donor.medicalConditions],['Medications',donor.medications],['Allergies',donor.allergies],['Donor notes',donor.notesAboutTheDonor],['Donor profile created',donor.createdAtDonorProfile],['Donor profile updated',donor.updatedAtDonorProfile]]);
            if(donorMissing){
                $('#profileRoleInformation').innerHTML=donorOnboarding();
                $('#donorOnboarding').onsubmit=async event=>{
                    event.preventDefault();const button=event.target.querySelector('button');
                    if(button.disabled)return;button.disabled=true;
                    try{
                        const created=await api('/api/donor-profile',{method:'POST',body:JSON.stringify(donorProfilePayload())});
                        if(!current())return;
                        donor=retainDonorProfile(created);donorMissing=false;
                        notice='Your donor profile was created by Identity Service.';
                        drawInformation();notify('Donor profile created.');
                    }catch(error){if(current()){notify(error.message,true);button.disabled=false;}}
                };
            }
        }else if(role==='HOSPITAL_STAFF'){
            $('#profileRoleInformation').innerHTML=profileFields([['Staff name',account.name],['Staff email',account.email||email],['Staff phone',account.phoneNumber],['Account role',account.role],['Hospital',hospital.hospitalName],['Hospital ID',hospital.id],['Hospital location',hospital.hospitalLocation],['Hospital phone',hospital.hospitalPhone],['Hospital status',profileBoolean(hospital.hospitalActive,'Active','Inactive')],['Hospital description',hospital.hospitalDescription]]);
        }else{
            $('#profileRoleInformation').innerHTML='<p class="hint">This section displays your own account information only.</p>'+profileFields([['Administrator name',account.name],['Email',account.email||email],['Phone number',account.phoneNumber],['Account role',account.role]]);
        }
    }
    $('#profileForm').onsubmit=async event=>{
        event.preventDefault();const button=event.target.querySelector('button');
        if(button.disabled||!current())return;
        const name=val('profileName').trim(),phoneNumber=val('profilePhone').trim();
        if(!name||!/^\d{8}$/.test(phoneNumber)){notify('Enter your full name and an eight-digit phone number.',true);return;}
        button.disabled=true;
        try{
            const updated=await api('/api/users/me',{method:'PUT',body:JSON.stringify({name,phoneNumber})});
            account=acceptAccountResponse(updated);
            if(!current())return;
            drawInformation();
            $('#profileName').value=account.name;$('#profilePhone').value=account.phoneNumber;
            try{
                account=await refreshAccount();
                if(current()){drawInformation();$('#profileName').value=account.name;$('#profilePhone').value=account.phoneNumber;notify('Profile updated and refreshed from Identity Service.');}
            }catch(error){if(current())notify('Profile saved, but account refresh failed. '+error.message,true);}
        }catch(error){if(current())notify(error.message,true);}
        finally{if(button.isConnected)button.disabled=false;}
    };
    $('#passwordForm').onsubmit=async event=>{
        event.preventDefault();const button=event.target.querySelector('button');
        if(button.disabled||!current())return;button.disabled=true;
        try{await api('/api/auth/change-password',{method:'PUT',body:JSON.stringify({currentPassword:val('oldPassword'),newPassword:val('newPassword')})});if(current()){event.target.reset();notify('Password updated successfully.');}}
        catch(error){if(current())notify(error.message,true);}finally{if(button.isConnected)button.disabled=false;}
    };
    for(const [form,path] of [...(role==='DONOR'?[['cprForm','/api/users/profile/cpr']]:[]),['imageForm','/api/users/profile/image']]){
        $('#'+form).onsubmit=async event=>{
            event.preventDefault();const button=event.target.querySelector('button');
            if(button.disabled||!current()||form==='cprForm'&&state.role!=='DONOR')return;
            button.disabled=true;
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
    if(role==='ADMIN')$('#deactivateMe').closest('section').hidden=true;
    $('#deactivateMe').onclick=async()=>{
        const button=$('#deactivateMe');
        if(button.disabled||!current()||!confirm('Deactivate your account? This action affects your ability to sign in.'))return;
        button.disabled=true;
        try{await api('/api/users/me',{method:'DELETE'});if(current()){signout();notify('Account deactivated.');}}
        catch(error){if(current())notify(error.message,true);}
        finally{if(button.isConnected)button.disabled=false;}
    };
    try{
        account=await refreshAccount();if(!current())return;
        if(role==='DONOR'){
            const loaded=await getDonorProfile();if(!current())return;
            donor=loaded||{};donorMissing=!loaded;
            notice=loaded?'Your donor profile was returned by Identity Service.':'Complete donor onboarding below after uploading your CPR document.';
        }else if(role==='HOSPITAL_STAFF'){
            if(!positiveId(account.hospitalId))throw Error('Your staff account needs an assigned hospital. Contact an administrator.');
            hospital=await api('/api/hospitals/'+encodeURIComponent(account.hospitalId));
            if(!current())return;
            notice='Account and assigned hospital were retrieved from Identity and Blood Request Service.';
        }else{
            notice='Your signed-in account details were returned by Identity during session validation. Fields absent from its response are marked Not available.';
        }
        if(!current())return;
        account=state.user||account;
        drawInformation();
        if(account.name&&!val('profileName'))$('#profileName').value=account.name;
        if(account.phoneNumber&&!val('profilePhone'))$('#profilePhone').value=account.phoneNumber;
    }catch(error){
        if(!current())return;
        notice='Unable to load profile information. Your account controls remain available. Use Refresh information to try again.';
        drawInformation();notify(error.message,true);
    }
}

let managementGeneration=0;
function managementScope(){
    const generation=++managementGeneration,token=state.token,page=state.page,visit=profileVisit,role=state.role;
    return ()=>generation===managementGeneration&&token===state.token&&page===state.page&&visit===profileVisit&&role===state.role;
}
async function verifiedStaff(){
    const me=await api('/api/users/me');
    validateSessionAccount(me);
    if(me.role!=='HOSPITAL_STAFF'||!positiveId(me.hospitalId))throw Error('Your staff account needs an assigned hospital. Contact an administrator.');
    state.user=me;return me;
}
function wireManagementSubmit(id,action,after,current,message){
    const form=$('#'+id);
    form.onsubmit=async event=>{
        event.preventDefault();const button=form.querySelector('button');
        if(button.disabled||!current())return;button.disabled=true;
        try{
            const result=await action();
            if(!current())return;
            form.reset();notify(message);
            await after(result);
        }catch(error){if(current())notify(error.message,true);}
        finally{if(button.isConnected)button.disabled=false;}
    };
}
function staffRequestPayload(hospitalId){
    const units=Number(val('units'));
    if(!positiveId(hospitalId)||!Number.isInteger(units)||units<1||val('neededDate')<utcToday())throw Error('Choose a valid hospital, date and positive whole number of units.');
    return {hospitalId,requiredBloodType:val('bloodType'),requiredBloodUnits:units,
        bloodRequestUrgency:val('urgency'),bloodNeededDate:val('neededDate'),bloodRequestDescription:val('description').trim()||null};
}
async function managementRequests(){
    if(!['HOSPITAL_STAFF','ADMIN'].includes(state.role)||!state.token)return;
    
    const current=managementScope(),staff=state.role==='HOSPITAL_STAFF';
    let editingRequest=null;
    $('#view').innerHTML=panel('Blood requests','Loading requests...');
    try{
        const me=staff?await verifiedStaff():null;
        const [rows]=await Promise.all([loadRequestRecords(staff?'/api/blood-requests/hospital':'/api/blood-requests?page=0&size=100&sortBy=id&direction=desc'),loadRequestProgress(current),loadHospitals({isCurrent:current})]);
        if(!current())return;
        if(!Array.isArray(rows)||staff&&rows.some(r=>r.hospitalId!==me.hospitalId))throw Error('Invalid hospital request response.');
        state.requests=rows;
        $('#view').innerHTML=(staff?panel('Create a blood request','<p class="hint">Assigned hospital ID: '+esc(me.hospitalId)+'</p><form id="requestForm"><div class="form-grid">'+select('bloodType','Required blood type',['A+','A-','B+','B-','AB+','AB-','O+','O-'])+field('units','Units needed','number','min="1" step="1" required')+select('urgency','Urgency',['LOW','MEDIUM','HIGH','CRITICAL'])+field('neededDate','Needed by','date','min="'+utcToday()+'" required')+'</div><div class="field"><label for="description">Description</label><textarea id="description"></textarea></div><button class="button">Create request</button></form>'):'')
            +panel(staff?'Hospital blood requests':'All blood requests',requestListTools('managerRequest')+'<button class="button secondary" id="refreshManagerRequests">Refresh requests</button><p class="hint">Request details are provided by the service. Fields not supplied are marked Not available.</p><div id="managerRequestTabs"></div><div class="grid" id="managerRequestGrid">'+rows.map(r=>'<article class="item" data-urgent="'+isUrgentRequest(r)+'" data-request-id="'+esc(r.id)+'" data-request-life="'+requestLifecycle(r)+'"><h3>Blood request #'+esc(r.id)+'</h3>'+urgentBadge(r)+statusPill(requestLifecycle(r).toUpperCase())+donorRequestFacts(r)+shareControls(r)+(staff&&r.bloodRequestStatus==='OPEN'?'<div class="actions"><button class="button secondary" data-request-edit="'+esc(r.id)+'">Edit request</button><button class="button outline" data-request-delete="'+esc(r.id)+'">Delete request</button><button class="button secondary" data-staff-request="'+esc(r.id)+'" data-status="CLOSED">Close request</button><button class="button outline" data-staff-request="'+esc(r.id)+'" data-status="CANCELLED">Cancel request</button></div>':'')+'</article>').join('')+'</div>'+'<div class="empty" id="managerRequestEmpty">No requests in this tab.</div><details><summary>Closed / Other (<span id="managerOtherCount">'+rows.filter(r=>requestLifecycle(r)==='other').length+'</span>)</summary><div class="grid" id="managerOtherGrid"></div></details>');
        function renderRequestTabs(){
            if(!current())return;
            const selected=selectedLifecycle('managementRequests');
            const visible=searchSortRequests(rows,val('managerRequestSearch'),val('managerRequestSort'));
            $('#managerRequestTabs').innerHTML=lifecycleTabs('managementRequests',visible,requestLifecycle);
            const cards=new Map([...document.querySelectorAll('[data-request-life]')].map(card=>{card.hidden=true;return [card.dataset.requestId,card];}));
            visible.forEach(request=>{
                const card=cards.get(String(request.id)),category=requestLifecycle(request),other=category==='other';
                card.dataset.requestLife=category;
                card.dataset.urgent=String(isUrgentRequest(request));
                card.querySelectorAll('.urgent-badge').forEach(badge=>badge.hidden=!isUrgentRequest(request));
                (other?$('#managerOtherGrid'):$('#managerRequestGrid')).appendChild(card);
                card.hidden=!other&&category!==selected;
            });
            $('#managerRequestEmpty').hidden=visible.some(r=>requestLifecycle(r)===selected);
            $('#managerOtherCount').textContent=visible.filter(r=>requestLifecycle(r)==='other').length;
            wireLifecycleTabs('managementRequests',renderRequestTabs);
        }
        renderRequestTabs();wireSharing();
        $('#managerRequestSearch').oninput=renderRequestTabs;$('#managerRequestSort').onchange=renderRequestTabs;
        $('#refreshManagerRequests').onclick=managementRequests;
        if(staff){
            wireManagementSubmit('requestForm',async()=>{
                const verified=await verifiedStaff();
                if(!current())throw Error('This page is no longer current.');
                return api('/api/blood-requests'+(editingRequest?'/'+editingRequest:''),{method:editingRequest?'PUT':'POST',body:JSON.stringify(staffRequestPayload(verified.hospitalId))});
            },managementRequests,current,'Blood request saved.');
            document.querySelectorAll('[data-request-edit]').forEach(button=>button.onclick=()=>{
                const request=rows.find(r=>String(r.id)===button.dataset.requestEdit);
                if(!current()||!request)return;
                editingRequest=request.id;
                for(const [key,value] of Object.entries({bloodType:request.requiredBloodType,units:request.requiredBloodUnits,urgency:request.bloodRequestUrgency,neededDate:request.neededDate,description:request.bloodRequestDescription}))$('#'+key).value=value??'';
                $('#requestForm').querySelector('button').textContent='Save request #'+request.id;
                $('#requestForm').scrollIntoView({behavior:'smooth'});
            });
            document.querySelectorAll('[data-request-delete]').forEach(button=>button.onclick=async()=>{
                if(!current()||button.disabled||!confirm('Delete request #'+button.dataset.requestDelete+'? Requests with bookings cannot be deleted.'))return;
                button.disabled=true;
                try{await api('/api/blood-requests/'+button.dataset.requestDelete,{method:'DELETE'});if(current()){await managementRequests();notify('Request deleted.');}}
                catch(error){if(current()){notify(error.message,true);button.disabled=false;}}
            });
            document.querySelectorAll('[data-staff-request]').forEach(button=>button.onclick=async()=>{
                if(button.disabled||!current()||!confirm('Change request #'+button.dataset.staffRequest+' to '+button.dataset.status+'?'))return;
                button.disabled=true;
                try{await api('/api/blood-requests/'+encodeURIComponent(button.dataset.staffRequest)+'/status?status='+encodeURIComponent(button.dataset.status),{method:'PATCH'});if(current()){notify('Request status updated.');await managementRequests();}}
                catch(error){if(current())notify(error.message,true);}
                finally{if(button.isConnected)button.disabled=false;}
            });
        }
    }catch(error){if(current()){$('#view').innerHTML=panel('Blood requests','<p class="hint">'+esc(error.message)+'</p>');}}
}
function staffSlotPayload(){
    const slotDate=val('slotDate'),startTime=val('slotStart'),endTime=val('slotEnd'),capacity=Number(val('slotCapacity'));
    const start=appointmentInstant(slotDate,startTime),end=appointmentInstant(slotDate,endTime);
    if(!slotDate||!startTime||!endTime||!Number.isFinite(start.getTime())||!Number.isFinite(end.getTime())||start<=new Date()||end<=start||!Number.isInteger(capacity)||capacity<1)throw Error('Choose a future slot, an end time after its start, and a positive whole capacity.');
    return {slotDate,startTime,endTime,capacity};
}
async function staffSlots(){
    if(state.role!=='HOSPITAL_STAFF'||!state.token)return;
    const current=managementScope();
    $('#view').innerHTML=panel('Hospital donation slots','Loading slots and booking counts...');
    try{
        const me=await verifiedStaff(),data=await api('/api/donation-slots/my-hospital/lifecycle');
        if(!current())return;
        if(!Array.isArray(data))throw Error('Invalid slot response.');
        const rows=data.map(({slot,...counts})=>({...slot,...counts}));
        if(rows.some(s=>s.hospitalId!==me.hospitalId))throw Error('Invalid hospital slot response.');
        state.slots=rows;
        $('#view').innerHTML=panel('Create a donation slot','<p class="hint">Assigned hospital ID: '+esc(me.hospitalId)+'</p><form id="slotForm"><div class="form-grid">'+field('slotDate','Appointment date (UTC)','date','min="'+utcToday()+'" required')+field('slotStart','Start time (UTC)','time','required')+field('slotEnd','End time (UTC)','time','required')+field('slotCapacity','Capacity','number','min="1" step="1" required')+'</div><button class="button">Create appointment slot</button></form>')
            +panel('Hospital appointment schedule','<button class="button secondary" id="refreshStaffSlots">Refresh slots</button><p class="hint">Full means capacity is reached. A slot is completed only after its end time, with at least one non-cancelled booking and all such bookings completed. In-progress and future closed slots remain under Closed / Other.</p><div id="staffSlotLifecycle"></div>');
        function cards(items){return items.length?'<div class="slot-grid">'+items.map(s=>'<article class="item"><h3>Slot #'+esc(s.id)+'</h3>'+slotFacts(s)+'<form data-slot-capacity="'+esc(s.id)+'"><label for="capacity'+esc(s.id)+'">Update capacity</label><input id="capacity'+esc(s.id)+'" type="number" min="'+Math.max(1,s.reservedCount||0)+'" step="1" value="'+esc(s.capacity)+'" required><button class="button secondary">Save capacity</button></form></article>').join('')+'</div>':'<div class="empty">No slots in this section.</div>';}
        function render(){
            if(!current())return;
            const selected=selectedLifecycle('staffSlots'),other=rows.filter(s=>slotLifecycle(s)==='other');
            $('#staffSlotLifecycle').innerHTML=lifecycleTabs('staffSlots',rows,slotLifecycle)+cards(rows.filter(s=>slotLifecycle(s)===selected))
                +'<details><summary>Closed / Other ('+other.length+')</summary>'+cards(other)+'</details>';
            wireLifecycleTabs('staffSlots',render);
            document.querySelectorAll('[data-slot-capacity]').forEach(form=>form.onsubmit=async event=>{
                event.preventDefault();const button=form.querySelector('button'),capacity=Number(form.querySelector('input').value);
                if(button.disabled||!current()||!Number.isInteger(capacity)||capacity<1)return;
                button.disabled=true;
                try{await api('/api/donation-slots/'+form.dataset.slotCapacity+'/capacity?capacity='+capacity,{method:'PUT'});if(current()){await staffSlots();notify('Capacity updated.');}}
                catch(error){if(current()){notify(error.message,true);button.disabled=false;}}
            });
        }
        render();$('#refreshStaffSlots').onclick=staffSlots;
        wireManagementSubmit('slotForm',()=>api('/api/donation-slots',{method:'POST',body:JSON.stringify(staffSlotPayload())}),staffSlots,current,'Donation slot created.');
    }catch(error){if(current())$('#view').innerHTML=panel('Hospital donation slots','<p class="hint">'+esc(error.message)+'</p><button class="button secondary" id="retryStaffSlots">Try again</button>');if(current())$('#retryStaffSlots').onclick=staffSlots;}
}
async function donorSlots(){
    const token=state.token,visit=profileVisit;
    const current=()=>state.page==='slots'&&state.token===token&&profileVisit===visit;
    $('#view').innerHTML=panel('Donation slots','Loading available requests and your slot history...');
    try{
        await requireDonorProfile();
        if(!current())return;
        let active=[],historyRows=[],available=[],selection=0;
        $('#view').innerHTML=panel('Donation slots','<label for="slotRequestChoice">Blood request for new appointments</label><select id="slotRequestChoice"><option value="">Choose an active request</option>'+active.map(r=>'<option value="'+esc(r.id)+'">Request #'+esc(r.id)+' · '+esc(r.hospitalName)+' · '+esc(r.requiredBloodType)+'</option>').join('')+'</select><button class="button secondary" id="refreshDonorSlots">Refresh slots and history</button><p class="hint">Active shows bookable slots for the chosen request and your upcoming booked slots. Completed/Expired history includes only slots tied to your bookings; counts describe all bookings in those slots without donor identities.</p><div id="donorSlotLifecycle"></div>');
        function render(){
            if(!current())return;
            const byId=new Map(historyRows.map(slot=>[slot.id,slot]));
            available.forEach(slot=>byId.set(slot.id,{...byId.get(slot.id),...slot}));
            const rows=[...byId.values()],selected=selectedLifecycle('donorSlots'),request=active.find(r=>String(r.id)===val('slotRequestChoice'));
            function cards(items){return items.length?'<div class="slot-grid">'+items.map(s=>'<article class="item"><h3>Slot #'+esc(s.id)+'</h3>'+slotFacts(s)+(request&&available.some(a=>a.id===s.id)&&slotLifecycle(s)==='active'?'<button class="button" data-book-slot="'+esc(s.id)+'">Choose appointment</button>':'')+'</article>').join('')+'</div>':'<div class="empty">No slots in this section. Choose an active request for new appointments.</div>';}
            const other=rows.filter(s=>slotLifecycle(s)==='other');
            $('#donorSlotLifecycle').innerHTML=lifecycleTabs('donorSlots',rows,slotLifecycle)+cards(rows.filter(s=>slotLifecycle(s)===selected))+'<details><summary>Closed / Other ('+other.length+')</summary>'+cards(other)+'</details>';
            wireLifecycleTabs('donorSlots',render);
            document.querySelectorAll('[data-book-slot]').forEach(button=>button.onclick=()=>donorBookingForm(request.id,Number(button.dataset.bookSlot)));
        }
        async function load(){
            const chosen=++selection,previous=val('slotRequestChoice');
            $('#donorSlotLifecycle').textContent='Loading slot availability and history...';
            try{
                const [requests,history]=await Promise.all([api('/api/blood-requests/browse'),api('/api/donation-slots/my-history')]);
                if(!current()||chosen!==selection)return;
                if(!Array.isArray(requests)||!Array.isArray(history))throw Error('Invalid slot history response.');
                active=requests.filter(r=>requestLifecycle(r)==='active');
                historyRows=history.map(({slot,...counts})=>({...slot,...counts}));
                $('#slotRequestChoice').innerHTML='<option value="">Choose an active request</option>'+active.map(r=>'<option value="'+esc(r.id)+'">Request #'+esc(r.id)+' · '+esc(r.hospitalName)+' · '+esc(r.requiredBloodType)+'</option>').join('');
                const id=active.some(r=>String(r.id)===previous)?previous:active.length?String(active[0].id):'';
                $('#slotRequestChoice').value=id;
                let warning='';
                try{available=id?await api('/api/donation-slots/available?bloodRequestId='+id):[];}
                catch(error){available=[];warning='New slot availability is unavailable. '+error.message;}
                if(!current()||chosen!==selection)return;
                render();if(warning)notify(warning,true);
            }catch(error){if(current()&&chosen===selection){$('#donorSlotLifecycle').textContent='Unable to load slots. Use Refresh slots and history to try again.';notify(error.message,true);}}
        }
        $('#slotRequestChoice').onchange=load;$('#refreshDonorSlots').onclick=load;await load();
    }catch(error){if(current()){$('#view').innerHTML=panel('Donation slots','<p class="hint">'+esc(error.message)+'</p><button class="button secondary" id="slotsProfile">My Profile</button>');$('#slotsProfile').onclick=()=>navigate('profile');}}
}
async function staffBookingWorkflow(){
    if(state.role!=='HOSPITAL_STAFF'||!state.token)return;
    const current=managementScope();
    $('#view').innerHTML=panel('Hospital donation schedule','Loading your hospital bookings...');
    try{
        const me=await verifiedStaff();let rows=[],requests=[],loading=false;
        $('#view').innerHTML=panel('Hospital donation schedule','<div class="form-grid"><div class="field"><label for="staffRequestChoice">Blood request</label><select id="staffRequestChoice"><option value="">All hospital requests</option></select></div>'+field('staffScheduleDate','Appointment date (Bahrain)','date')+'</div><div class="actions"><button class="button secondary" id="schedulePrevious">Previous day</button><button class="button secondary" id="scheduleNext">Next day</button><button class="button secondary" id="scheduleAll">All dates</button><button class="button" id="refreshStaffApplications">Refresh schedule</button></div><p class="hint">Appointments are grouped by Bahrain date. Only your assigned hospital is included.</p><div id="staffRequestBookings">Loading...</div>');
        function render(){
            if(!current())return;
            const requestId=val('staffRequestChoice'),date=val('staffScheduleDate');
            const filtered=rows.filter(b=>(!requestId||String(b.bloodRequestId)===requestId)&&(!date||bahrainDate(b.donationBookingDate,b.donationBookingTime)===date)).slice().sort((a,b)=>appointmentInstant(a.donationBookingDate,a.donationBookingTime)-appointmentInstant(b.donationBookingDate,b.donationBookingTime)||a.id-b.id);
            const groups=new Map();filtered.forEach(b=>{const day=bahrainDate(b.donationBookingDate,b.donationBookingTime);if(!groups.has(day))groups.set(day,[]);groups.get(day).push(b);});
            $('#staffRequestBookings').innerHTML=(requestId?requestProgress({id:Number(requestId)}):'')+(groups.size?[...groups].map(([day,bookings])=>'<section><h3>'+esc(day)+' · Bahrain ('+bookings.length+')</h3><div class="grid">'+bookings.map(b=>'<article class="item"><h4>Booking #'+esc(b.id)+'</h4>'+profileFields([['Request',b.bloodRequestId],['Slot',b.donationSlotId],['Donor profile ID',b.donorProfileId],['Appointment',bahrainAppointment(b.donationBookingDate,b.donationBookingTime)],['Status',b.donationBookingStatus]])+(bookingTransitions(b.donationBookingStatus).length?'<label>Change status<select data-staff-booking="'+esc(b.id)+'"><option value="">Choose status</option>'+bookingTransitions(b.donationBookingStatus).map(status=>'<option>'+status+'</option>').join('')+'</select></label>':'')+'</article>').join('')+'</div></section>').join(''):'<div class="empty">No bookings match this request/date.</div>');
            document.querySelectorAll('[data-staff-booking]').forEach(control=>control.onchange=async()=>{
                const booking=rows.find(b=>String(b.id)===control.dataset.staffBooking),status=control.value;
                if(loading||control.disabled||!current()||!booking||!bookingTransitions(booking.donationBookingStatus).includes(status))return;
                if(!confirm('Change booking #'+booking.id+' to '+status+'?')){control.value='';return;}
                control.disabled=true;
                try{await api('/api/donation-bookings/'+booking.id+'/status?status='+encodeURIComponent(status),{method:'PUT'});if(current()){await load();notify('Booking status updated.');}}
                catch(error){if(current()){control.disabled=false;control.value='';notify(error.message,true);}}
            });
        }
        async function load(){
            if(loading)return;loading=true;$('#staffRequestBookings').textContent='Loading hospital schedule...';
            try{
                const [bookings,requestRows]=await Promise.all([api('/api/donation-bookings/my-hospital'),api('/api/blood-requests/hospital'),loadRequestProgress(current)]);
                if(!current())return;
                if(!Array.isArray(bookings)||bookings.some(b=>b.hospitalId!==me.hospitalId)||!Array.isArray(requestRows)||requestRows.some(r=>r.hospitalId!==me.hospitalId))throw Error('Unexpected hospital response.');
                rows=bookings;requests=requestRows;
                const selected=val('staffRequestChoice');
                $('#staffRequestChoice').innerHTML='<option value="">All hospital requests</option>'+requests.map(r=>'<option value="'+esc(r.id)+'">Request #'+esc(r.id)+' · '+esc(r.bloodRequestStatus)+'</option>').join('');
                $('#staffRequestChoice').value=selected;render();
            }catch(error){if(current()){$('#staffRequestBookings').textContent='Unable to load schedule. Use Refresh schedule to retry.';notify(error.message,true);}}
            finally{loading=false;}
        }
        $('#staffRequestChoice').onchange=render;$('#staffScheduleDate').onchange=render;
        function shiftDate(days){
            const now=new Date(),base=val('staffScheduleDate')||bahrainDate(now.toISOString().slice(0,10),now.toISOString().slice(11,19));
            const next=new Date(base+'T12:00:00Z');next.setUTCDate(next.getUTCDate()+days);$('#staffScheduleDate').value=next.toISOString().slice(0,10);render();
        }
        $('#schedulePrevious').onclick=()=>shiftDate(-1);$('#scheduleNext').onclick=()=>shiftDate(1);
        $('#scheduleAll').onclick=()=>{$('#staffScheduleDate').value='';render();};$('#refreshStaffApplications').onclick=load;await load();
    }catch(error){if(current()){$('#view').innerHTML=panel('Hospital donation schedule','<p class="error">'+esc(error.message)+'</p><button class="button secondary" id="retrySchedule">Try again</button>');$('#retrySchedule').onclick=staffBookingWorkflow;}}
}
function hospitalPayload(prefix='h'){
    const hospitalName=val(prefix+'Name').trim(),hospitalLocation=val(prefix+'Location').trim(),hospitalPhone=val(prefix+'Phone').trim();
    if(!hospitalName||!hospitalLocation||!hospitalPhone)throw Error('Enter a hospital name, location and phone.');
    return {hospitalName,hospitalLocation,hospitalPhone,hospitalDescription:val(prefix+'Description').trim()||null};
}
async function admin(){
    if(state.role!=='ADMIN'||!state.token){navigate('overview');return;}
    const current=managementScope();
    $('#view').innerHTML=panel('Create hospital','<form id="hospitalForm"><div class="form-grid">'+field('hName','Hospital name','text','required')+field('hLocation','Location','text','required')+field('hPhone','Phone','tel','required')+'</div>'+field('hDescription','Description')+'<button class="button">Create hospital</button></form>')
        +panel('Manage hospitals','<div class="toolbar"><input id="adminHospitalSearch" aria-label="Search hospitals" placeholder="Search hospital name or location"><select id="adminHospitalSort" aria-label="Sort hospitals"><option value="name">Hospital name</option><option value="location">Location</option><option value="status">Active first</option></select></div><div id="adminHospitals">Loading hospitals...</div>')
        +panel('Register hospital staff','<form id="staffRegistrationForm"><div class="form-grid">'+field('staffName','Full name','text','required')+field('staffEmail','Email','email','required')+field('staffPhone','Phone (8 digits)','tel','pattern="[0-9]{8}" required')+field('staffPassword','Initial password','password','autocomplete="new-password" required')+'</div><button class="button">Register staff</button></form><p class="hint">Identity sends the verification email. The staff member must verify their email before signing in.</p><div id="staffRegistrationResult" role="status"></div>')
        +panel('Assign staff to hospital','<form id="staffAssignmentForm"><div class="form-grid">'+field('assignmentUserId','Verified staff user ID','number','min="1" step="1" required')+'<div class="field"><label for="assignmentHospitalId">Hospital</label><select id="assignmentHospitalId" required><option value="">Loading hospitals...</option></select></div></div><p class="hint">Use the ID returned by registration or deliberately enter a known staff ID. Search users below to find an existing staff account.</p><button class="button">Assign hospital</button></form><div id="staffAssignmentResult" role="status"></div>')
        +'<p class="hint">Administrator accounts are protected from deactivation. Other accounts can be managed below.</p>';
    $('#view').insertAdjacentHTML('beforeend',panel('Manage accounts','<form id="userSearchForm"><label for="userSearch">Search by name or email</label><input id="userSearch" maxlength="100"><button class="button">Search users</button></form><div id="adminUsers"></div>'));
    let userPage=0,userRequest=0;
    async function loadUsers(){
        const request=++userRequest;
        try{
            const result=await api('/api/users?search='+encodeURIComponent(val('userSearch').trim())+'&page='+userPage);
            if(!current()||request!==userRequest)return;
            $('#adminUsers').innerHTML=result.content.map(u=>'<article class="item"><h3>'+esc(u.name)+'</h3>'+profileFields([['ID',u.id],['Email',u.email],['Role',u.role],['Status',u.status],['Email verified',u.emailVerified]])+(u.role==='ADMIN'?'':'<button class="button secondary" data-account-id="'+esc(u.id)+'" data-action="'+(u.status==='ACTIVE'?'deactivate':'reactivate')+'">'+(u.status==='ACTIVE'?'Deactivate':'Reactivate')+'</button>')+'</article>').join('')+'<div class="actions"><button id="usersPrevious" class="button secondary" '+(result.first?'disabled':'')+'>Previous</button><button id="usersNext" class="button secondary" '+(result.last?'disabled':'')+'>Next</button></div>';
            $('#usersPrevious').onclick=()=>{userPage--;loadUsers();}; $('#usersNext').onclick=()=>{userPage++;loadUsers();};
            document.querySelectorAll('[data-account-id]').forEach(button=>button.onclick=async()=>{
                if(button.disabled||!confirm(button.dataset.action+' account #'+button.dataset.accountId+'?'))return;
                button.disabled=true;
                try{await api('/api/users/'+button.dataset.accountId+'/'+button.dataset.action,{method:'PUT'});if(current())await loadUsers();}
                catch(error){if(current()){notify(error.message,true);button.disabled=false;}}
            });
        }catch(error){if(current())notify(error.message,true);}
    }
    $('#userSearchForm').onsubmit=event=>{event.preventDefault();userPage=0;loadUsers();};
    loadUsers();
    wireManagementSubmit('hospitalForm',()=>api('/api/hospitals',{method:'POST',body:JSON.stringify(hospitalPayload())}),()=>loadAdminHospitals(current),current,'Hospital created.');
    wireManagementSubmit('staffRegistrationForm',()=>{
        const name=val('staffName').trim(),email=val('staffEmail').trim(),phoneNumber=val('staffPhone').trim(),password=val('staffPassword');
        if(!name||!email||!/^\d{8}$/.test(phoneNumber)||!password)throw Error('Complete staff details with an eight-digit phone number.');
        return api('/api/users/staff',{method:'POST',body:JSON.stringify({name,email,phoneNumber,password})});
    },user=>{
        if(!positiveId(user?.id)||user.role!=='HOSPITAL_STAFF')throw Error('Identity returned an invalid staff response. Verify the outcome before registering again.');
        $('#assignmentUserId').value=user.id;
        $('#staffRegistrationResult').textContent='Registered staff user ID: '+user.id+'. Email verification is required.';
    },current,'Staff registered.');
    wireManagementSubmit('staffAssignmentForm',async()=>{
        const userId=Number(val('assignmentUserId')),hospitalId=Number(val('assignmentHospitalId'));
        if(!positiveId(userId)||!state.hospitals.some(h=>h.id===hospitalId))throw Error('Enter a valid staff user ID and choose a returned hospital.');
        const assigned=await api('/api/users/'+userId+'/hospital/'+hospitalId,{method:'PUT'});
        if(assigned?.id!==userId||assigned.hospitalId!==hospitalId)throw Error('Identity returned an unexpected assignment. Verify the outcome before retrying.');
        return assigned;
    },user=>{
        if(!positiveId(user?.id)||user.role!=='HOSPITAL_STAFF'||!positiveId(user.hospitalId))throw Error('Identity returned an invalid assignment response. Verify the assignment before retrying.');
        $('#staffAssignmentResult').textContent='Staff user #'+user.id+' assigned to hospital #'+user.hospitalId+'.';
    },current,'Staff hospital assignment saved.');
    await loadAdminHospitals(current);
}
async function loadAdminHospitals(current=managementScope()){
    if(state.role!=='ADMIN')return;
    try{
        const rows=await api('/api/hospitals');
        if(!current())return;
        if(!Array.isArray(rows))throw Error('Invalid hospital list.');
        state.hospitals=rows;
        $('#assignmentHospitalId').innerHTML='<option value="">Choose a hospital</option>'+rows.map(h=>'<option value="'+esc(h.id)+'">'+esc(h.hospitalName)+' (#'+esc(h.id)+')</option>').join('');
        function renderTable(){
            if(!current())return;
            const visible=searchSortHospitals(rows,val('adminHospitalSearch'),val('adminHospitalSort'));
        $('#adminHospitals').innerHTML='<button class="button secondary" id="refreshAdminHospitals">Refresh hospitals</button>'+(visible.length?'<div class="table-wrap"><table><thead><tr><th>ID</th><th>Name</th><th>Location</th><th>Status</th><th>Actions</th></tr></thead><tbody>'+visible.map(h=>'<tr><td>'+esc(h.id)+'</td><td>'+esc(h.hospitalName)+'</td><td>'+esc(h.hospitalLocation)+'</td><td>'+statusPill(h.hospitalActive?'ACTIVE':'INACTIVE')+'</td><td><button class="button secondary" data-h-toggle="'+esc(h.id)+'" data-active="'+h.hospitalActive+'">'+(h.hospitalActive?'Deactivate':'Activate')+'</button> <button class="button outline" data-h-edit="'+esc(h.id)+'">Edit</button></td></tr>').join('')+'</tbody></table></div>':'<div class="empty">No hospitals match this search.</div>');
        $('#refreshAdminHospitals').onclick=()=>loadAdminHospitals(current);
        document.querySelectorAll('[data-h-toggle]').forEach(button=>button.onclick=async()=>{
            if(button.disabled||!current())return;
            const action=button.dataset.active==='true'?'deactivate':'activate';
            if(!confirm(action+' hospital #'+button.dataset.hToggle+'?'))return;
            button.disabled=true;
            try{await api('/api/hospitals/'+encodeURIComponent(button.dataset.hToggle)+'/'+action,{method:'PATCH'});if(current()){notify('Hospital status updated.');await loadAdminHospitals(current);}}
            catch(error){if(current())notify(error.message,true);}finally{if(button.isConnected)button.disabled=false;}
        });
        document.querySelectorAll('[data-h-edit]').forEach(button=>button.onclick=()=>{
            if(!current())return;
            const hospital=rows.find(h=>String(h.id)===button.dataset.hEdit);if(!hospital)return;
            $('#adminHospitals').innerHTML='<form id="editHospital"><h3>Edit hospital #'+esc(hospital.id)+'</h3>'+field('editName','Name','text','required')+field('editLocation','Location','text','required')+field('editPhone','Phone','tel','required')+field('editDescription','Description')+'<button class="button">Save changes</button><button class="button secondary" type="button" id="cancelHospitalEdit">Cancel</button></form>';
            for(const key of ['Name','Location','Phone','Description'])$('#edit'+key).value=hospital['hospital'+key]||'';
            $('#cancelHospitalEdit').onclick=()=>loadAdminHospitals(current);
            wireManagementSubmit('editHospital',()=>api('/api/hospitals/'+encodeURIComponent(hospital.id),{method:'PUT',body:JSON.stringify(hospitalPayload('edit'))}),()=>loadAdminHospitals(current),current,'Hospital saved.');
        });
        }
        renderTable();$('#adminHospitalSearch').oninput=renderTable;$('#adminHospitalSort').onchange=renderTable;
    }catch(error){if(current()){$('#adminHospitals').textContent='Unable to load hospitals.';notify(error.message,true);}}
}

function auth(){if(state.token){navigate('overview');return}$('#view').innerHTML=`<div class="auth-layout"><div class="auth-tabs"><button class="button" id="tabLogin">Sign in</button><button class="button secondary" id="tabRegister">Create account</button><button class="button secondary" id="tabForgot">Forgot password</button></div><div id="authContent"></div></div>`;$('#tabLogin').onclick=showLogin;$('#tabRegister').onclick=showRegister;$('#tabForgot').onclick=showForgot;showLogin()}
function showLogin(){
    $('#authContent').innerHTML=panel('Welcome back',`<form id="loginForm">${field('email','Email','email','autocomplete="username" required')}${field('password','Password','password','autocomplete="current-password" required')}<p class="hint">Your account role is provided securely by Identity Service.</p><button class="button" style="width:100%">Sign in</button></form>`);
    const form=$('#loginForm');
    form.onsubmit=async event=>{
        event.preventDefault();
        const button=form.querySelector('button');
        if(button.disabled)return;
        const generation=++sessionGeneration;
        button.disabled=true;button.textContent='Signing in…';
        try{
            const result=await identityRequest('/api/auth/login',{
                method:'POST',body:JSON.stringify({email:val('email').trim(),password:val('password')})
            });
            if(generation!==sessionGeneration||!form.isConnected)return;
            if(typeof result.token!=='string'||!result.token.trim()
                    ||typeof result.email!=='string'||!result.email.trim()
                    ||!['DONOR','HOSPITAL_STAFF','ADMIN'].includes(result.role)){
                throw Error('Identity Service returned an invalid login response.');
            }
            const account=await identityRequest('/api/users/me',{token:result.token});
            if(generation!==sessionGeneration||!form.isConnected)return;
            validateSessionAccount(account);
            if(account.email!==result.email||account.role!==result.role){
                throw Error('Account details changed during login. Please sign in again.');
            }
            initializeSession(result.token,account);
            form.reset();navigate(resumePage());notify('Signed in successfully.');
        }catch(error){
            if(generation!==sessionGeneration||!form.isConnected)return;
            clearSession();notify(error.message,true);
        }finally{
            if(form.isConnected){button.disabled=false;button.textContent='Sign in';}
        }
    };
}
function showRegister(){
    $('#authContent').innerHTML=panel('Join Med Support',`<form id="registerForm">${field('rName','Full name','text','autocomplete="name" required')}${field('rEmail','Email','email','autocomplete="email" required')}${field('rPhone','Phone number','tel','pattern="[0-9]{8}" maxlength="8" required')}${field('rPassword','Password','password','autocomplete="new-password" required')}<p class="hint">Public registration creates a donor account. Hospital staff accounts are created by an administrator.</p><button class="button">Create account</button></form><p class="hint">Verify your email before signing in.</p>`);
    const form=$('#registerForm'),generation=sessionGeneration;
    const current=()=>form.isConnected&&generation===sessionGeneration&&!state.token;
    form.onsubmit=async event=>{
        event.preventDefault();const button=form.querySelector('button');
        if(button.disabled||!current())return;
        const name=val('rName').trim(),email=val('rEmail').trim(),phoneNumber=val('rPhone').trim(),password=val('rPassword');
        if(!name||!email||!/^\d{8}$/.test(phoneNumber)||!password){notify('Complete your donor details with an eight-digit phone number.',true);return;}
        button.disabled=true;
        try{
            await api('/api/auth/register',{method:'POST',body:JSON.stringify({name,email,phoneNumber,password,role:'DONOR'})});
            if(current()){form.reset();showLogin();notify('Donor registration submitted. Check your email for verification.');}
        }catch(error){if(current())notify(error.message,true);}
        finally{if(button.isConnected)button.disabled=false;}
    };
}
function showForgot(){
    $('#authContent').innerHTML=panel('Recover your account',`<form id="forgotForm">${field('forgotEmail','Email address','email','autocomplete="email" required')}<button class="button">Send reset link</button></form>`);
    const form=$('#forgotForm'),generation=sessionGeneration;
    const current=()=>form.isConnected&&generation===sessionGeneration&&!state.token;
    form.onsubmit=async event=>{
        event.preventDefault();const button=form.querySelector('button');
        if(button.disabled||!current())return;
        const email=val('forgotEmail').trim();if(!email){notify('Enter your email address.',true);return;}
        button.disabled=true;
        try{
            await api('/api/auth/forgot-password?email='+encodeURIComponent(email),{method:'POST'});
            if(current()){form.reset();notify('If the email is registered, a reset link will be sent.');}
        }catch(error){if(current())notify(error.message,true);}
        finally{if(button.isConnected)button.disabled=false;}
    };
}
function signout(){clearSession();navigate('auth');}
$('#logout').onclick=signout;
async function startFrontend(){
    const params=new URLSearchParams(location.search),token=params.get('token');
    const resetToken=params.get('resetToken');
    if(params.has('resetToken')){
        params.delete('resetToken');
        history.replaceState(null,'',location.pathname+(params.size?'?'+params.toString():'')+location.hash);
    }
    let verification=null;
    if(params.has('token')){
        // Remove the one-time token from the address bar without logging or storing it.
        params.delete('token');
        try{const query=params.toString();history.replaceState(null,'',location.pathname+(query?'?'+query:'')+location.hash);}catch{}
        try{
            await api('/api/auth/verify-email?token='+encodeURIComponent(token||''));
            verification={message:'Email verified. You can now sign in.',error:false};
        }catch(error){verification={message:error.message,error:true};}
    }
    // Restore before showing the verification result so navigation cannot erase it.
    if(resetToken){clearSession();navigate('auth');}else await restoreSession();
    if(verification)notify(verification.message,verification.error);
    if(resetToken){
        $('#view').innerHTML=panel('Reset password','<form id="resetPasswordForm">'+field('resetNewPassword','New password','password','minlength="8" autocomplete="new-password" required')+'<button class="button">Reset password</button></form>');
        $('#resetPasswordForm').onsubmit=async event=>{
            event.preventDefault();const form=event.target,button=form.querySelector('button');
            if(button.disabled)return;button.disabled=true;
            try{await api('/api/auth/reset-password',{method:'POST',body:JSON.stringify({passwordResetToken:resetToken,newPassword:val('resetNewPassword')})});if(form.isConnected){clearSession();navigate('auth');notify('Password reset. Sign in with your new password.');}}
            catch(error){if(form.isConnected){notify(error.message,true);button.disabled=false;}}
        };
    }
}
function connectNotifications(){
    if(!state.token||(state.socket&&notificationSocketToken===state.token))return;
    clearTimeout(notificationReconnect);
    const token=state.token,generation=sessionGeneration;
    notificationSocketToken=token;
    try{
        const url=new URL(window.MED_SUPPORT_CONFIG.apiBaseUrls.notification);
        url.protocol=url.protocol==='https:'?'wss:':'ws:';url.pathname='/ws/notifications';url.search='';url.hash='';
        const socket=new WebSocket(url);state.socket=socket;
        socket.onopen=()=>{if(token===state.token&&generation===sessionGeneration)socket.send(JSON.stringify({token}));else socket.close();};
        socket.onmessage=event=>{
            if(token!==state.token||generation!==sessionGeneration)return;
            try{const data=JSON.parse(event.data);if(Array.isArray(data.content)){state.notifications=data.content;if(state.page==='notifications'&&notificationPage===0)drawNotifications(data);}}
            catch{}
        };
        socket.onclose=()=>{if(state.socket===socket)state.socket=null;if(token===state.token&&generation===sessionGeneration)notificationReconnect=setTimeout(connectNotifications,10000);};
        socket.onerror=()=>socket.close();
    }catch{notificationReconnect=setTimeout(()=>{if(token===state.token)connectNotifications();},10000);}
}
let notificationPage=0;
function drawNotifications(data){
    const container=$('#notificationItems');if(!container)return;
    container.innerHTML=data.content.map(n=>'<article class="item"><h3>'+esc(n.title)+'</h3><p>'+esc(n.message)+'</p><small>'+esc(n.createdAt)+(n.bookingId?' · Booking #'+esc(n.bookingId):'')+'</small></article>').join('')||'<div class="empty">No notifications yet.</div>';
    $('#notificationsPrevious').disabled=data.first;$('#notificationsNext').disabled=data.last;
}
async function notifications(){
    if(!state.token){navigate('auth');return;}
    notificationPage=0;
    $('#view').innerHTML=panel('Your notifications','<p class="hint">Only your notifications are shown. New messages arrive automatically while connected.</p><div id="notificationItems">Loading...</div><div class="actions"><button class="button secondary" id="notificationsPrevious">Previous</button><button class="button secondary" id="notificationsNext">Next</button><button class="button" id="refreshNotifications">Refresh</button></div>');
    async function load(){
        const token=state.token,page=notificationPage,visit=profileVisit;
        try{const result=await api('/api/notifications/my?page='+page);if(state.page==='notifications'&&state.token===token&&profileVisit===visit&&notificationPage===page)drawNotifications(result);}
        catch(error){if(state.page==='notifications'&&profileVisit===visit)notify(error.message,true);}
    }
    $('#notificationsPrevious').onclick=()=>{if(notificationPage>0){notificationPage--;load();}};
    $('#notificationsNext').onclick=()=>{notificationPage++;load();};$('#refreshNotifications').onclick=load;
    connectNotifications();await load();
}
async function departments(){
    if(!state.token){navigate('auth');return;}
    const token=state.token,visit=profileVisit,current=()=>state.page==='departments'&&state.token===token&&profileVisit===visit;
    $('#view').innerHTML=panel('Hospital departments','Loading...');
    try{
        const hospitals=await api('/api/hospitals');if(!current())return;
        $('#view').innerHTML=panel('Hospital departments','<label for="departmentHospital">Hospital</label><select id="departmentHospital">'+hospitals.map(h=>'<option value="'+esc(h.id)+'">'+esc(h.hospitalName)+'</option>').join('')+'</select><div id="departmentList"></div>'+(state.role==='HOSPITAL_STAFF'?'<form id="departmentForm">'+field('departmentName','Department name','text','maxlength="100" required')+field('departmentDescription','Description','text','maxlength="500"')+'<button class="button">Add to my hospital</button></form>':''));
        if(!hospitals.length){$('#departmentList').textContent='No hospitals available.';return;}
        async function load(){
            const id=val('departmentHospital');
            try{const rows=await api('/api/hospitals/'+id+'/departments');if(current()&&val('departmentHospital')===id)$('#departmentList').innerHTML=rows.map(d=>'<article class="item"><h3>'+esc(d.name)+'</h3><p>'+esc(d.description)+'</p></article>').join('')||'<p class="hint">No departments.</p>';}
            catch(error){if(current())notify(error.message,true);}
        }
        $('#departmentHospital').onchange=load;
        if(state.role==='HOSPITAL_STAFF'){
            const account=await refreshAccount();if(!current())return;
            if(!positiveId(account.hospitalId))throw Error('Your staff account needs an assigned hospital.');
            $('#departmentHospital').value=String(account.hospitalId);
            $('#departmentForm').onsubmit=async event=>{
                event.preventDefault();const button=event.target.querySelector('button');if(button.disabled)return;button.disabled=true;
                try{await api('/api/hospitals/'+account.hospitalId+'/departments',{method:'POST',body:JSON.stringify({name:val('departmentName').trim(),description:val('departmentDescription').trim()||null})});if(current()){event.target.reset();$('#departmentHospital').value=String(account.hospitalId);await load();notify('Department added.');}}
                catch(error){if(current())notify(error.message,true);}finally{if(button.isConnected)button.disabled=false;}
            };
        }
        await load();
    }catch(error){if(current())notify(error.message,true);}
}
startFrontend();
