// Bulma Toggle Button
document.addEventListener('DOMContentLoaded', () => {
    const $navbarBurgers = Array.prototype.slice.call(document.querySelectorAll('.navbar-burger'), 0);
    if ($navbarBurgers.length > 0) {
        $navbarBurgers.forEach(el => {
            el.addEventListener('click', () => {
                const target = el.dataset.target;
                const $target = document.getElementById(target);
                el.classList.toggle('is-active');
                $target.classList.toggle('is-active');
            });
        });
    }
});

// Calendar
const calendars = bulmaCalendar.attach('[type="date"]', {
    'lang': 'en',
    'color': 'dark',
    'type': 'datetime',
    'dateFormat': 'YYYY-MM-DD',
    'timeFormat': 'HH:mm:ss',
    'displayMode': 'default'
});


// Axios Interceptor
const http = axios.create({baseURL: `/api/`})
http.interceptors.request.use((config) => {
    const token = localStorage.getItem("token")
    if (typeof token === 'string') {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
}, error => Promise.reject(error))

// Page Transitions
let context = {
    'task': 0
}

async function moveTo(path, cleanup = true) {
    // Cleanup
    if (cleanup) {
        document.querySelectorAll(".cleanup").forEach(elm => elm.innerHTML = '')
        document.querySelectorAll("input,textarea,select").forEach(elm => elm.value = "")
    }

    if (location.hash.substring(1) !== 'task') path ||= location.hash.substring(1)
    path ||= 'tasks'
    // Update navbar
    if (localStorage.getItem("token") === null) {
        document.querySelectorAll(".nav-signed-in").forEach(elm => elm.classList.add("is-hidden"))
        document.querySelectorAll(".nav-not-signed-in").forEach(elm => elm.classList.remove("is-hidden"))
        if (path !== 'signup') path = 'signin'
    } else {
        document.querySelectorAll(".nav-signed-in").forEach(elm => elm.classList.remove("is-hidden"))
        document.querySelectorAll(".nav-not-signed-in").forEach(elm => elm.classList.add("is-hidden"))
        document.getElementById("nav-name").innerText = localStorage.getItem("username")
    }
    // Move
    if (path === 'tasks') updateTaskList()
    document.querySelectorAll("section").forEach(elm => elm.classList.add("is-hidden"))
    document.getElementById(`sec-${path}`).classList.remove("is-hidden")
    location.hash = `#${path}`
}

moveTo()

// Utility
function showErrors(id, response) {
    let html = ''
    for (let desc of response.description) {
        html += `<p class="help is-danger">${desc}</p>`
    }
    document.getElementById(id).innerHTML = html
}

// Actions: Users
async function signin() {
    const name = document.getElementById('signin-name').value
    const password = document.getElementById('signin-password').value
    try {
        const resp = await http.post('/credential', {'name': name, 'password': password})
        localStorage.setItem('token', resp.data.data.token)
        localStorage.setItem('username', name)
        moveTo('tasks')
    } catch (err) {
        showErrors('signin-errors', err.response.data)
    }
}

async function signup() {
    const name = document.getElementById('signup-name').value
    const password = document.getElementById('signup-password').value
    try {
        await http.post('/users', {'name': name, 'password': password})
        moveTo('signin')
    } catch (err) {
        showErrors('signup-errors', err.response.data)
    }
}

async function signout() {
    localStorage.clear()
    moveTo('signin')
}

async function updatePassword() {
    const name = localStorage.getItem("username")
    const password = document.getElementById('settings-password').value
    try {
        await http.put('/users/me', {'name': name, 'password': password})
        moveTo('tasks')
    } catch (err) {
        showErrors('settings-errors', err.response.data)
    }
}

async function deleteAccount() {
    try {
        await http.delete('/users/me')
        localStorage.clear()
        moveTo('signin')
    } catch (err) {
        showErrors('settings-errors', err.response.data)
    }
}

// Actions: Tasks
async function updateTaskListWith(tasks) {
    let html = ''
    for (let task of tasks) {
        html += `<div class="card m-2">
            <header class="card-header">
                <p class="card-header-title">${task.title}</p>
                <span class="card-header-icon tags has-addons">
                    <span class="tag is-dark">status</span>
                    <span class="tag is-light">${task.state}</span>
                </span>
            </header>
            <div class="card-content">
                <div class="content">
                    ${task.description}
                    <br>
                    Created at <time>${task.created_at}</time>
                </div>
            </div>
            <footer class="card-footer">
                <a href="#" class="card-footer-item" onclick="showDetails(${task.id})">Details</a>
                <a href="#" class="card-footer-item" onclick="showDetails(${task.id})">Edit</a>
            </footer>
        </div>`
    }
    document.getElementById('tasks-container').innerHTML = html
}

async function updateTaskList() {
    try {
        const resp = await http.get('/tasks')
        const tasks = resp.data.data
        updateTaskListWith(tasks)
    } catch (err) {
        showErrors('generic-errors', err.response.data)
    }
}

async function searchTasks() {
    let state = []
    for (let option of document.getElementById('tasks-query-state').options) {
        if (option.selected) state.push(option.value)
    }
    const tag = document.getElementById('tasks-query-tag').value
    const page = document.getElementById('tasks-query-page').value
    const query = {}
    if (tag !== '') query.tag = tag
    if (page !== '') query.page = page
    if (state.length !== 0) query.state = state.join()
    try {
        const resp = await http.get(`/tasks`, {
            params: query
        })
        const task = resp.data.data
        updateTaskListWith(task)
    } catch (err) {
        console.log(err)
        showErrors('generic-errors', err.response.data)
    }
}

function showDetailsWith(task) {
    document.getElementById('task-title').value = task.title
    document.getElementById('task-description').value = task.description
    document.getElementById('task-state').value = task.state
    document.getElementById('task-created-at').value = task.created_at || ""
    document.getElementById('task-deadline').value = task.deadline || ""
    document.getElementById('task-completed-at').value = task.completed_at || ""
    document.getElementById('task-cycle').value = task.cycle || ""
    let htmlUsers = ""
    for (let user of task.users) {
        htmlUsers += `<div class="control">
            <div class="tags has-addons">
                <p class="tag is-light">${user.name}</p>
                <a class="tag is-delete is-danger" onclick="removeUser('${user.name}')"></a>
            </div>
        </div>`
    }
    let htmlTags = ""
    for (let tag of task.tags) {
        htmlTags += `<div class="control">
            <div class="tags has-addons">
                <p class="tag is-light">${tag}</p>
                <a class="tag is-delete is-danger" onclick="removeTag('${tag}')"></a>
            </div>
        </div>`
    }
    document.getElementById('task-users').innerHTML = htmlUsers
    document.getElementById('task-tags').innerHTML = htmlTags
}

async function showDetails(id) {
    context.task = id
    try {
        const resp = await http.get(`/tasks/${context.task}`)
        const task = resp.data.data
        showDetailsWith(task)
        moveTo('task', false)
    } catch (err) {
        console.log(err)
        showErrors('generic-errors', err.response.data)
    }
}

async function createTask() {
    const title = document.getElementById('newtask-title').value
    const description = document.getElementById('newtask-description').value
    const state = document.getElementById('newtask-state').value
    try {
        await http.post('/tasks', {
            'title': title,
            'description': description,
            'state': state,
        })
        moveTo('tasks')
    } catch (err) {
        showErrors('newtask-errors', err.response.data)
    }
}

async function updateTask() {
    const title = document.getElementById('task-title').value
    const description = document.getElementById('task-description').value
    const state = document.getElementById('task-state').value
    try {
        await http.post(`/tasks/${context.task}`, {
            'title': title,
            'description': description,
            'state': state,
        })
        moveTo('tasks')
    } catch (err) {
        showErrors('task-errors', err.response.data)
    }
}

async function addUser() {
    const name = document.getElementById('task-user-new').value
    try {
        const resp = await http.post(`/tasks/${context.task}/users`, `"${name}"`, {
            headers: {"Content-type": "application/json"}
        })
        document.getElementById('task-user-new').value = ""
        showDetailsWith(resp.data.data)
    } catch (err) {
        showErrors('task-errors', err.response.data)
    }
}

async function addTag() {
    const name = document.getElementById('task-tag-new').value
    try {
        const resp = await http.post(`/tasks/${context.task}/tags`, `"${name}"`, {
            headers: {"Content-type": "application/json"}
        })
        document.getElementById('task-tag-new').value = ""
        showDetailsWith(resp.data.data)
    } catch (err) {
        showErrors('task-errors', err.response.data)
    }
}

async function removeUser(name) {
    try {
        const resp = await http.delete(`/tasks/${context.task}/users/${name}`)
        showDetailsWith(resp.data.data)
    } catch (err) {
        showErrors('task-errors', err.response.data)
    }
}

async function removeTag(name) {
    try {
        const resp = await http.delete(`/tasks/${context.task}/tags/${name}`)
        showDetailsWith(resp.data.data)
    } catch (err) {
        showErrors('task-errors', err.response.data)
    }
}
