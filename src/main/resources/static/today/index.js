let cardTemplate = `<div class="card w-fit  bg-base-100 shadow-xl img-container"  style="margin-top: 20px">
                        <figure class="figure-padding" >
                            <img src="" class="rounded-xl" alt=""/>
                        </figure>
                        <div id="card-body-height">
                            <div class="card-actions opt-btn-container ">
                                <button class="btn horrible btn-circle btn-ghost" >🥵</button>
                                <button class="btn downvote btn-circle btn-ghost" >😤</button>
                                <button class="btn upvote btn-circle btn-ghost"  >🤣</button>
                            </div>
                        </div>
                    </div>`


const getImgUrl = "/img/today/raw", voteUrl = "/img/vote"

function getToday() {
    let resp = null;
    let uuid = localStorage.getItem("uuid")
    if (uuid === null) {
        uuid = Math.random().toString(36);
        localStorage.setItem("uuid", uuid)
    }

    $.ajax({
        url: `${getImgUrl}?uuid=${uuid}`,
        type: "GET",
        async: false,
        success: function (data) {
            resp = data;
        },
        error: function () {
            resp = [];
        }
    })
    console.log(resp)
    return resp;
}


function vote(name, up = true) {
    let uuid = localStorage.getItem("uuid")
    if (uuid === null) {
        uuid = Math.random().toString(36);
        localStorage.setItem("uuid", uuid)
    }


    $.ajax({
        url: `${voteUrl}?uuid=${uuid}`,
        type: "POST",
        data: {
            name: name,
            up: up
        },
        success: function (data) {
            console.log(data)
        },
        error: function () {
            alert("error")
        }
    })
}

function upVote(name) {
    vote(name, true)
}

function downVote(name) {
    vote(name, false)
}

function horribleVote(name) {
    vote(name, false)
}

let arr = getToday()


// wait window load
window.onload = function () {
    // id="today"
    let today = document.getElementById("today")
    console.assert(today !== null, "today is null")

    if (arr.length === 0) {
        return
    }

    // remove default card
    let defaultCard = document.getElementById("default")
    today.removeChild(defaultCard)

    for (let i = 0; i < arr.length; i++) {
        let card = document.createElement("div")
        card.innerHTML = cardTemplate

        let img = card.querySelector("img")
        img.src = arr[i].url

        // set click event
        let upvote = card.querySelector(".upvote")
        upvote.onclick = function () {
            upVote(arr[i].name)
        }
        let downvote = card.querySelector(".downvote")
        downvote.onclick = function () {
            downVote(arr[i].name)
        }

        let horrible = card.querySelector(".horrible")
        horrible.onclick = function () {
            horribleVote(arr[i].name)
        }

        upvote.id = `${arr[i].name}-upvote`
        downvote.id = `${arr[i].name}-downvote`
        horrible.id = `${arr[i].name}-horrible`

        today.appendChild(card)
    }
}
