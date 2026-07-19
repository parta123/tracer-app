(function(){
    if(window.__accountMessagePopupStarted){
        return;
    }

    window.__accountMessagePopupStarted = true;

    let activeMessage = null;
    let polling = false;
    let pollTimer = null;
    let pollUntil = Number(localStorage.getItem("accountMessagePollUntil") || 0);
    const POLL_WINDOW_MS = 3 * 60 * 1000;
    const POLL_INTERVAL_MS = 6000;

    function shouldPoll(){
        return Date.now() < pollUntil;
    }

    function armPolling(duration=POLL_WINDOW_MS){
        pollUntil = Math.max(pollUntil, Date.now() + duration);
        localStorage.setItem("accountMessagePollUntil", String(pollUntil));

        if(!pollTimer){
            pollTimer = setInterval(function(){
                if(!shouldPoll() && !activeMessage){
                    clearInterval(pollTimer);
                    pollTimer = null;
                    return;
                }
                pollMessage();
            }, POLL_INTERVAL_MS);
        }

        pollMessage();
    }

    window.startAccountMessagePolling = armPolling;

    function ensurePopup(){
        if(document.getElementById("accountMessageToast")){
            return;
        }

        const style = document.createElement("style");
        style.textContent = `
            #accountMessageToast{
                position:fixed;
                top:22px;
                right:22px;
                z-index:99999;
                display:none;
                max-width:min(360px, calc(100vw - 32px));
            }
            #accountMessageToast.show{
                display:block;
            }
            .account-message-card{
                background:#ffffff;
                border:1px solid rgba(226,232,240,.95);
                border-radius:16px;
                box-shadow:0 18px 45px rgba(15,23,42,.22);
                padding:16px;
                color:#111827;
                font-family:inherit;
            }
            .account-message-top{
                display:flex;
                align-items:flex-start;
                justify-content:space-between;
                gap:14px;
                margin-bottom:10px;
            }
            .account-message-label{
                font-size:11px;
                font-weight:700;
                letter-spacing:.12em;
                color:#e60023;
                margin-bottom:4px;
            }
            .account-message-sender{
                font-size:14px;
                font-weight:600;
                color:#111827;
            }
            .account-message-close{
                width:30px;
                height:30px;
                border:none;
                border-radius:10px;
                background:#f8fafc;
                color:#111827;
                cursor:pointer;
                font-size:19px;
                line-height:1;
            }
            .account-message-close:hover{
                background:#fff1f3;
                color:#d6001c;
            }
            .account-message-text{
                white-space:pre-wrap;
                word-break:break-word;
                font-size:14px;
                line-height:1.55;
                color:#334155;
                margin:8px 0 12px;
            }
            .account-message-image{
                display:none;
                width:100%;
                max-height:210px;
                object-fit:contain;
                border-radius:12px;
                background:#f8fafc;
                margin:0 0 12px;
            }
            .account-message-ok{
                width:100%;
                border:none;
                border-radius:12px;
                padding:11px 14px;
                background:#e60023;
                color:#fff;
                cursor:pointer;
                font-size:14px;
                font-weight:600;
            }
            @media(max-width:640px){
                #accountMessageToast{
                    top:14px;
                    right:14px;
                    left:14px;
                    max-width:none;
                }
            }
        `;
        document.head.appendChild(style);

        const popup = document.createElement("div");
        popup.id = "accountMessageToast";
        popup.innerHTML = `
            <div class="account-message-card">
                <div class="account-message-top">
                    <div>
                        <div class="account-message-label">PESAN</div>
                        <div class="account-message-sender" id="accountMessageSender">-</div>
                    </div>
                    <button class="account-message-close" id="accountMessageClose" type="button" aria-label="Mengerti">&times;</button>
                </div>
                <div class="account-message-text" id="accountMessageText"></div>
                <img class="account-message-image" id="accountMessageImage" alt="Lampiran pesan">
                <button class="account-message-ok" id="accountMessageOk" type="button">Mengerti</button>
            </div>
        `;
        document.body.appendChild(popup);

        document.getElementById("accountMessageOk").addEventListener("click", acknowledgeMessage);
        document.getElementById("accountMessageClose").addEventListener("click", acknowledgeMessage);
    }

    function showMessage(message){
        ensurePopup();

        activeMessage = message;

        document.getElementById("accountMessageSender").textContent = message.sender_fullname || message.sender_username || "Admin";
        document.getElementById("accountMessageText").textContent = message.text || "";

        const image = document.getElementById("accountMessageImage");

        if(message.image_path){
            image.src = message.image_path;
            image.style.display = "block";
        }else{
            image.removeAttribute("src");
            image.style.display = "none";
        }

        document.getElementById("accountMessageToast").classList.add("show");
    }

    async function pollMessage(){
        if(activeMessage || polling){
            return;
        }

        polling = true;

        try{
            const response = await fetch("/account_message_poll", {
                cache:"no-store"
            });

            if(response.status === 401 || response.status === 403){
                return;
            }

            const result = await response.json();

            if(result && result.success && result.message){
                showMessage(result.message);
            }

        }catch(error){
        }finally{
            polling = false;
        }
    }

    async function acknowledgeMessage(){
        if(!activeMessage){
            return;
        }

        const id = activeMessage.id;

        try{
            await fetch("/account_message_ack", {
                method:"POST",
                headers:{
                    "Content-Type":"application/json"
                },
                body:JSON.stringify({
                    id:id
                })
            });
        }catch(error){
        }

        document.getElementById("accountMessageToast").classList.remove("show");
        activeMessage = null;

        armPolling(30000);
    }

    window.addEventListener("storage", function(event){
        if(event.key === "accountMessagePollUntil"){
            const value = Number(event.newValue || 0);
            if(value > Date.now()){
                pollUntil = value;
                armPolling(value - Date.now());
            }
        }
    });

    document.addEventListener("DOMContentLoaded", function(){
        ensurePopup();
        if(shouldPoll()){
            armPolling(pollUntil - Date.now());
        }
    });
})();