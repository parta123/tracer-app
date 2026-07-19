const beep = document.getElementById("beep");
const errorSound = document.getElementById("errorSound");
const highValueSound = new Audio("/static/high.mp3");
const resultPanel = document.getElementById("result-panel");
const resultContent = document.getElementById("result-content");

let scanner;
let currentCameraId = null;
let cameras = [];
const DOMAIN_URL = "https://pindai.asyscntr.com";

let gunScannerActive = false; // 🆕 Track mode Gun Scanner

let CURRENT_SPOT = "-";
let CURRENT_CODE = "-";
let CURRENT_STATION = "";
let CODE_WARNING_SHOWN = false;
let mobileScanInFlight = false;
let mobileLastResi = "";
let mobileLastScanAt = 0;

const MOBILE_DUPLICATE_COOLDOWN_MS = 2500;

function isMobileDevice(){
  return /android|iphone|ipad|ipod|mobile/i.test(
    navigator.userAgent || ""
  );
}

function chooseDefaultCamera(devices){
  const list = Array.isArray(devices) ? devices : [];

  if(!list.length){
    return null;
  }

  const backCamera =
    list.find(camera =>
      /back|rear|environment|belakang|kamera belakang/i.test(
        camera.label || ""
      )
    );

  if(backCamera){
    return backCamera.id;
  }

  if(isMobileDevice() && list.length > 1){
    return list[list.length - 1].id;
  }

  return list[0].id;
}

function normalizeScannedResi(resi){
  return String(resi || "").replace(/[^A-Za-z0-9]/g, "");
}

function claimMobileScan(resi){
  const cleanedResi = normalizeScannedResi(resi);
  const now = Date.now();

  if(!cleanedResi){
    return "";
  }

  if(mobileScanInFlight){
    return "";
  }

  if(
    cleanedResi === mobileLastResi &&
    now - mobileLastScanAt < MOBILE_DUPLICATE_COOLDOWN_MS
  ){
    return "";
  }

  mobileScanInFlight = true;
  mobileLastResi = cleanedResi;
  mobileLastScanAt = now;

  return cleanedResi;
}

function releaseMobileScanLock(){
  mobileScanInFlight = false;
  mobileLastScanAt = Date.now();
}

// === START SCANNER ===
function startScanner(cameraId) {
  if(!scanner){
		scanner = new Html5Qrcode("reader");
	}

  const cameraConfig =
    cameraId === "__BACK_CAMERA__"
      ? { facingMode: "environment" }
      : { deviceId: { exact: cameraId } };

  scanner.start(
    cameraConfig,
	
    {
      fps: 15,
    },
    handleScanSuccess,
    handleScanError
  ).catch(err => {
    console.error("❌ Gagal start kamera:", err);
    if(cameraId === "__BACK_CAMERA__"){
      const fallbackCameraId = chooseDefaultCamera(cameras);
      if(fallbackCameraId){
        currentCameraId = fallbackCameraId;
        startScanner(fallbackCameraId);
        return;
      }
    }
    alert("Tidak bisa mengakses kamera. Pastikan sudah memberi izin.");
  });
}

// === HANDLE SCAN SUCCESS ===
function handleScanSuccess(resi) {


  if(gunScannerActive && idleBg){
      idleBg.classList.add("slide-up");
  }

  if(gunScannerActive){
      document.body.classList.remove("icon-dark");
      document.body.classList.add("icon-light");
  }

  console.log("✅ Resi terdeteksi:", resi);
  
  processResi(resi);

	return;

  setTimeout(() => flashTorch(false), 100);

  scanner.pause();

  fetch("/scan_resi", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
	  resi: resi,
	  spot: CURRENT_SPOT,
	  kode: CURRENT_CODE,
	  station: CURRENT_STATION
	})
  })
  .then(res => res.json())
  .then(data => {

	  if (data.success) {

		const badges = data.data.badges || [];

		const hasProblem =
			badges.includes("COMPLAINT") ||
			badges.includes("AUTOCLAIM");

		const isHighValue =
			badges.includes("HIGH VALUE");

		if (isHighValue) {

			highValueSound.currentTime = 0;

			highValueSound.play().catch(()=>{});

		} else if (hasProblem) {

			errorSound.currentTime = 0;

			errorSound.play().catch(()=>{});

		} else {

			beep.currentTime = 0;

			beep.play().catch(()=>{});

		}

		showResult(data.data);

	  } else if (res.status === 401) {
		alert("Token expired. Silakan upload HAR baru.");
		window.location.href = "/upload_har";
	  } else {
		showError(data.error);
	  }
	})
  .catch(err => {
    console.error("❌ Error koneksi:", err);
    showError("Gagal koneksi ke server");
  })
  .finally(() => {
	  scanner.resume();
	});
}


// === HANDLE SCAN ERROR ===
function handleScanError(err) {
  console.warn("⚠️ Scan error:", err);
}


function formatRupiah(angka) {
  return angka.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

const DEFAULT_SCAN_SETTINGS = {
  badges: {
    OUTGOING: true,
    INCOMING: true,
    COMPLAINT: true,
    AUTOCLAIM: true,
    "CLAIM INTERNAL": true,
    "HIGH VALUE": true
  },
  fields: {
    resi: true,
    dp_out: true,
    implant: true,
    seller: true,
    collect_staff: true,
    last_station: true,
    last_status: true,
    harga: true,
    waktu_scan: true,
    barang: true
  }
};

let scanSettings = DEFAULT_SCAN_SETTINGS;

fetch("/scan_settings?format=json")
  .then(res => res.json())
  .then(data => {
    if(data.success && data.settings){
      scanSettings = data.settings;
    }
  })
  .catch(()=>{});

function scanFieldEnabled(key){
  return !scanSettings.fields || scanSettings.fields[key] !== false;
}

function scanBadgeEnabled(badge){
  return !scanSettings.badges || scanSettings.badges[badge] !== false;
}

// === SHOW RESULT PANEL ===
function showResult(data) {
  console.log("[DEBUG] Data dari server:", data); // <-- Tambahkan debug log
  const resultPanel = document.getElementById("result-panel");
  const resultContent = document.getElementById("result-content");

   const badgesHtml = (data.badges || [])
   .filter(scanBadgeEnabled)
   .map(badge => {

		let className = "badge";

		let icon = "";

		if (badge === "OUTGOING") {

			className += " badge-green";
			icon = "/static/img/icon/outgoing.png";

		}

		if (badge === "INCOMING") {

			className += " badge-green";
			icon = "/static/img/icon/incoming.png";

		}

		if (badge === "COMPLAINT") {

			className += " badge-blue";
			icon = "/static/img/icon/complaint.png";

		}

		if (badge === "AUTOCLAIM") {

			className += " badge-red";
			icon = "/static/img/icon/claim.png";

		}

		if (badge === "CLAIM INTERNAL") {

			className += " badge-red";
			icon = "/static/img/icon/claim.png";

		}

		if (badge === "HIGH VALUE") {

			className += " badge-gold";
			icon = "/static/img/icon/high.png";

		}

		return `

			<span
				class="${className}"
				id="${badge === 'COMPLAINT' ? 'complaintBadge' : ''}"
			>

				<img
					src="${icon}"
					class="badge-icon"
				>

				<span class="badge-text">
					${badge}
				</span>

			</span>

		`;

	}).join("");

  const mappingSource = String(data.mapping_source || "sprinter").toLowerCase();
  const hasMapping = String(data.implant || "-").trim() !== "-";

  const infoRow = (key, icon, label, value, extraClass = "") => {
    if(!scanFieldEnabled(key)){
      return "";
    }

    const textValue =
      value || "-";

    const sourceField = mappingSource === "seller"
      ? "seller"
      : "collect_staff";
    const shouldBlink = hasMapping && (
      key === "implant" || key === sourceField
    ) && String(textValue).trim() !== "-";

    return `
      <div class="info-row ${extraClass}">
        <div class="info-label">
          <img src="${icon}">
          <span>${label}</span>
        </div>
        <div class="info-value ${key === "resi" ? "resi-value" : ""} ${shouldBlink ? "blink-red-white" : ""}" id="${key === "barang" ? "barang" : ""}">
          ${textValue}
        </div>
      </div>
    `;
  };

  const rowsHtml = [
    infoRow("resi", "/static/img/icon/resi.png", "RESI", data.resi, "resi-row"),
    infoRow("dp_out", "/static/img/icon/dpout.png", "DP OUT", data.dp_out),
    infoRow("implant", "/static/img/icon/station.png", "IMPLANT", data.implant || "-"),
    infoRow("seller", "/static/img/icon/seller.png", "SELLER", data.seller),
    infoRow("collect_staff", "/static/img/icon/sprinter.png", "SPRINTER", data.collect_staff, "sprinter-row"),
    infoRow("last_station", "/static/img/icon/station.png", "LAST STATION", data.last_station),
    infoRow("last_status", "/static/img/icon/status.png", "LAST STATUS", data.last_status),
    infoRow("harga", "/static/img/icon/harga.png", "HARGA", `Rp ${formatRupiah(data.harga || 0)}`),
    infoRow("waktu_scan", "/static/img/icon/waktu.png", "WAKTU SCAN", data.waktu_scan),
    infoRow("barang", "/static/img/icon/barang.png", "BARANG", data.barang || "-", "barang-row")
  ].join("");
  
  resultContent.innerHTML = `

	<div class="badges">
		${badgesHtml}
	</div>

	<div class="info-list">
		${rowsHtml}

	</div>
	`;
	
	resultPanel.classList.add("show");
	
	const barangEl = document.getElementById("barang");

	if(barangEl){

	barangEl.addEventListener("click", () => {

		document
			.getElementById("barang-overlay")
			.classList
			.add("show");

		document
			.getElementById("barang-popup-text")
			.innerText = data.barang;

	});

	}

	document
		.getElementById("barang-overlay")
		.onclick = () => {

			document
				.getElementById("barang-overlay")
				.classList
				.remove("show");

		};
  
  // === Add event for COMPLAINT badge ===
  const complaintBadge = document.getElementById("complaintBadge");
  if (complaintBadge) {
    complaintBadge.addEventListener("click", function (event) {
      event.stopPropagation(); // 🚫 prevent closePanel
      toggleComplaintPanel(data);
    });
  }

}

// === SHOW ERROR PANEL ===
function showError(msg) {
  resultContent.innerHTML = `<p style="color:red;">❌ ${msg}</p>`;
  resultPanel.classList.add("show");
  
}



// === CLOSE PANEL ===
function closePanel(){

  resultPanel.classList.remove("show");

  if(gunScannerActive && idleBg){
      idleBg.classList.remove("slide-up");
  }

  if(gunScannerActive){
      document.body.classList.remove("icon-light");
      document.body.classList.add("icon-dark");
  }

}

// === FLASH SCREEN ANIMATION ===
function flashScreen() {
  const flash = document.createElement("div");
  flash.classList.add("flash");
  document.body.appendChild(flash);
  setTimeout(() => flash.remove(), 300);
}

// === SWITCH CAMERA ===
function switchCamera() {
  if (cameras.length < 2) {
    alert("Hanya ada 1 kamera.");
    return;
  }
  scanner.stop().then(() => {
    currentCameraId = cameras.find(c => c.id !== currentCameraId).id;
    startScanner(currentCameraId);
  });
}

const gunBtn = document.getElementById("gun-btn");

if(gunBtn){

  gunBtn.addEventListener(
    "click",
    toggleGunScanner
  );

}

function toggleGunScanner() {

  const host = window.location.hostname;

  // cek apakah sekarang pakai IP
  const isIP = /^\d+\.\d+\.\d+\.\d+$/.test(host);

  if (!isIP) {

    // sekarang DOMAIN → pindah ke IP
    fetch("/get_ip")
      .then(res => res.json())
      .then(data => {

        window.location.href = "http://" + data.ip + ":5001/?gun=1";

      });

  } else {

    // sekarang IP → balik ke domain
    window.location.href = "https://regis.asyscntr.com";

  }

}





function processResi(resi) {

	resi = claimMobileScan(resi);

	if(!resi){
		return;
	}
        

	if(
		CURRENT_CODE === "-"
		&&
		!CODE_WARNING_SHOWN
	){

		const warningOverlay =
			document.getElementById("warning-overlay");

		warningOverlay.classList.add("show");

		document
			.getElementById("warning-cancel")
			.onclick = () => {

				warningOverlay.classList.remove("show");
				releaseMobileScanLock();

			};

		document
			.getElementById("warning-confirm")
			.onclick = () => {

				warningOverlay.classList.remove("show");
				CODE_WARNING_SHOWN = true;

				processResiLanjut(resi);

			};

		return;

	}

	processResiLanjut(resi);

}


function processResiLanjut(resi) {
	

  // hapus spasi dan karakter aneh dari scanner
  resi = normalizeScannedResi(resi);

  console.log("Resi bersih:", resi);
  console.log("KIRIM SPOT:", CURRENT_SPOT);

  fetch("/scan_resi", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
	  resi: resi,
	  spot: CURRENT_SPOT,
	  kode: CURRENT_CODE,
	  station: CURRENT_STATION
	})
  })
  .then(res => res.json())
  .then(data => {
	  if (data.success) {

		const badges = data.data.badges || [];

		const hasProblem =
			badges.includes("COMPLAINT") ||
			badges.includes("AUTOCLAIM");

		const isHighValue =
			badges.includes("HIGH VALUE");

		if (isHighValue) {

			highValueSound.currentTime = 0;

			highValueSound.play().catch(()=>{});

		} else if (hasProblem) {

			errorSound.currentTime = 0;

			errorSound.play().catch(()=>{});

		} else {

			beep.currentTime = 0;

			beep.play().catch(()=>{});

		}

		showResult(data.data);

	  } else {
		alert("❌ " + (data.error || "Resi gagal diproses"));
	  }
	})
  .catch(err => {
    console.error("❌ Error koneksi:", err);
    alert("Gagal koneksi ke server");
  })
  .finally(() => {
    releaseMobileScanLock();
  });
}

const urlParams = new URLSearchParams(window.location.search);

const idleBg = document.getElementById("idle-bg");

if (urlParams.get("gun") === "1") {

    gunScannerActive = true;

    document.body.classList.add("gun-mode");

    if(idleBg){
        document.body.classList.remove("icon-light");
        document.body.classList.add("icon-dark");
        idleBg.style.display = "flex";
    }

}

const scanBox = document.getElementById("scan-box");

if (scanBox) {

  setTimeout(() => {
    scanBox.focus();
  }, 300);

  scanBox.addEventListener("keydown", function(e){

    if(e.key === "Enter"){

      const resi = scanBox.value.trim();

      if(resi.length >= 10){

        processResi(resi);

        scanBox.value = "";

      }

    }

  });

}

document.addEventListener("click",(e)=>{

  // kalau klik di spot input atau di panel spot jangan pindah fokus
  if(e.target.id === "spot-input" || e.target.closest("#spot-box")) return;

  setTimeout(() => {
    scanBox.focus({ preventScroll:true });
  }, 10);

});


// === INIT CAMERA ===
if (!gunScannerActive) {

  Html5Qrcode.getCameras().then(devices => {
    cameras = devices.map(d => ({ id: d.id, label: d.label }));
    currentCameraId = isMobileDevice()
      ? "__BACK_CAMERA__"
      : chooseDefaultCamera(cameras);
    startScanner(currentCameraId);
  }).catch(err => {
    console.error("❌ Tidak ada kamera:", err);
    alert("Tidak ada kamera yang ditemukan.");
  });

}


function flashTorch(state) {
  try {
    const track = scanner.getRunningTrack();
    const capabilities = track.getCapabilities();
    if (capabilities.torch) {
      track.applyConstraints({ advanced: [{ torch: state }] });
    }
  } catch (err) {
    console.warn("⚠️ Torch tidak didukung:", err);
  }
}

function manualInput() {
  const resi = prompt("Masukkan nomor resi:").trim();
  if (resi.length >= 10) {
    fetch("/scan_resi", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
		  resi: resi,
		  spot: CURRENT_SPOT,
		  kode: CURRENT_CODE,
		  station: CURRENT_STATION
		})
    })
    .then(res => {
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      return res.json();
    })
    .then(data => {
      console.log("[DEBUG manualInput] response:", data);
      if (data.success) {
        const badges = data.data.badges || [];

		const hasProblem =
			badges.includes("COMPLAINT") ||
			badges.includes("AUTOCLAIM");

		const isHighValue =
			badges.includes("HIGH VALUE");

		if (isHighValue) {

			highValueSound.currentTime = 0;

			highValueSound.play().catch(()=>{});

		} else if (hasProblem) {

			errorSound.currentTime = 0;

			errorSound.play().catch(()=>{});

		} else {

			beep.currentTime = 0;

			beep.play().catch(()=>{});

		}

		showResult(data.data);
      } else {
        alert("❌ " + (data.error || "Resi gagal diproses"));
      }
    })
    .catch(err => {
      console.warn("[WARNING manualInput]:", err);
      // Cek kalau sudah ada hasil, jangan alert error
      if (!document.getElementById("result-panel").classList.contains("show")) {
        alert("❌ Terjadi kesalahan: " + err.message);
      }
    });
  } else {
    alert("❌ Nomor resi harus minimal 10 karakter");
  }
}

let showingComplaint = false; // 🔥 track status panel complaint

function toggleComplaintPanel(data) {
  const content = document.getElementById("result-content");
  const badgeComplaint = document.querySelector(".badge-complaint");

  if (!showingComplaint) {
    // === TAMPILKAN COMPLAINT PANEL ===
    let complaintsHtml = "";
    if (data.complaints && data.complaints.length > 0) {
    const sortedComplaints = data.complaints.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
	sortedComplaints.forEach((c, index) => {
	  complaintsHtml += `
		<div class="complaint-item">
		  <p><b>Resi:</b> ${c.waybillNo}</p>
		  <p><b>Complaint:</b> ${c.secondTypeName.split("-")[0]}</p>
		  <p><b>Status:</b> ${c.workOrderStatusName}</p>
		  <p><b>Regist:</b> ${c.createTime}</p>
		  <p><b>Jenis Tiket:</b> ${c.workTypeName}</p>
		  <hr>
		</div>
	  `;
	});
    } else {
      complaintsHtml = "<p>📭 Tidak ada data Complaint</p>";
    }

    content.innerHTML = `
	  <div class="badges">
		<span class="badge badge-red">COMPLAINT</span>
	  </div>
	  <div id="complaint-wrapper" style="max-height: 50vh; overflow-y: auto;">
		${complaintsHtml}
	  </div>
	`;
    badgeComplaint.classList.add("badge-red");
    showingComplaint = true;
  } else {
    // === KEMBALIKAN KE HASIL SCAN ===
    showResult(data);
    showingComplaint = false;
  }
}


// PANEL TOGGLE

const panel = document.getElementById("top-panel");
const toggle = document.getElementById("toggle-panel");

function updateToggleLabel(){

  if(!toggle){
    return;
  }

  const parts = [];
  const code = String(CURRENT_CODE || "-").trim();

  if(code && code !== "-"){
    parts.push(code);
  }

  if(parts.length){
    toggle.innerText = parts.join(" | ");
    toggle.classList.add("kode-active", "has-label");
    return;
  }

  toggle.innerText = "⬇";
  toggle.classList.remove("kode-active", "has-label");

}

toggle.onclick = () => {

  panel.classList.toggle("show");

  // panah hilang
  toggle.classList.add("hide");

};


document.addEventListener("click",(e)=>{

  if(!panel.contains(e.target) && e.target !== toggle){

      panel.classList.remove("show");

      // panah muncul lagi
      toggle.classList.remove("hide");

  }

});


const spotInput = document.getElementById("spot-input");
const kodeOverlay = document.getElementById("kode-overlay");

const kodeInput = document.getElementById("kode-input");
const kodeKeyboardBtn =
	document.getElementById("kode-keyboard-btn");
const kodeResetBtn =
	document.getElementById("kode-reset-btn");
	
kodeInput.addEventListener("click", function(e){

    e.stopPropagation();

});
const spotFile = document.getElementById("spot-file");



spotInput.addEventListener("click", function(){

    kodeOverlay.classList.add("show");

    setTimeout(() => {

        kodeInput.focus();

    }, 150);

});

kodeInput.addEventListener("keydown", function(e){

    if(e.key === "Enter"){

        const kode = kodeInput.value.trim();

        if(!kode) return;

        CURRENT_CODE = kode;
		
		toggle.classList.add("kode-active");
		updateToggleLabel();

        spotInput.value = kode;

        kodeOverlay.classList.remove("show");

        kodeInput.value = "";

        panel.classList.remove("show");

        toggle.classList.remove("hide");

        scanBox.focus();

    }

});

kodeOverlay.addEventListener("click", function(e){

    if(e.target.id === "kode-overlay"){

        kodeOverlay.classList.remove("show");

    }

});

if(kodeKeyboardBtn){

	kodeKeyboardBtn.addEventListener("click", () => {

		kodeInput.setAttribute(
			"inputmode",
			"text"
		);

		kodeInput.focus();

		setTimeout(() => {

			kodeInput.click();

		}, 50);

	});

}

if(kodeResetBtn){

	kodeResetBtn.addEventListener("click", () => {

		CURRENT_CODE = "-";
		CODE_WARNING_SHOWN = false;

		spotInput.value = "";

		updateToggleLabel();

		kodeOverlay.classList.remove("show");

		kodeInput.value = "";

		kodeInput.setAttribute(
			"inputmode",
			"none"
		);

		scanBox.focus();

	});

}
