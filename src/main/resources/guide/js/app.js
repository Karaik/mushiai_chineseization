import csvContent from '../data.inline.csv?raw';
import easterArtUrl from '../images/egg.webp';

const avatarAssets = import.meta.glob('../images/head/*.webp', { eager: true, import: 'default' });
const avatarMap = new Map(Object.entries(avatarAssets).map(([filePath, url]) => [filePath.split('/').pop(), url]));

const ROLES = [
    { id: 'all', label: '全部显示', style: 'role-gold' },
    { id: 'supervisor', label: '监修', style: 'role-red' },
    { id: 'coder', label: '程序', style: 'role-blue' },
    { id: 'artist', label: '美工', style: 'role-purple' },
    { id: 'trans', label: '翻译', style: 'role-green' },
    { id: 'proof', label: '校对', style: 'role-cyan' },
    { id: 'polish', label: '润色', style: 'role-pink' },
    { id: 'test', label: '测试', style: 'role-orange' }
];

const NODES = [
        { id: 'egg', label: '🎉🎉🎉', roles: ['egg'], x: 52, y: 38 },
        { id: '鳥谷真琴厨', roles: ['supervisor', 'proof'], x: 27.03, y: 12.65 },
        { id: 'DevSeeD', roles: ['coder'], x: 74.95, y: 11.05 },
        { id: '喵喵酱', roles: ['supervisor', 'trans', 'proof', 'polish', 'test'], x: 32, y: 28 },
        { id: '这位同学', roles: ['coder', 'trans', 'proof', 'polish', 'test'], x: 68, y: 24 },
        { id: '百分百原味胖次', roles: ['artist', 'proof', 'test'], x: 52, y: 38 },
        { id: 'ニャ森', roles: ['trans', 'proof'], x: 23.47, y: 46.95 },
        { id: '小白', roles: ['trans', 'proof', 'polish'], x: 36.04, y: 45.64 },
        { id: 'Ayachi00', roles: ['trans', 'polish'], x: 72.57, y: 50.44 },
        { id: '绅士君', roles: ['trans', 'polish'], x: 82.08, y: 39.68 },
        { id: '橘猫', roles: ['trans'], x: 8, y: 32 },
        { id: '情报弱者', roles: ['trans'], x: 6.53, y: 41.86 },
        { id: 'バカ', roles: ['trans'], x: 6, y: 58 },
        { id: '星洲鯨鯊。', roles: ['trans'], x: 92, y: 22 },
        { id: 'なずな', roles: ['polish', 'test'], x: 58.91, y: 55.38 },
        { id: '森凉℃', roles: ['proof', 'test'], x: 48, y: 68 },
        { id: 'nayuta', roles: ['proof'], x: 26.34, y: 60.47 },
        { id: '夜寒', roles: ['proof'], x: 32.08, y: 69.48 },
        { id: 'ダレソカレ', roles: ['proof'], x: 12.18, y: 68.31 },
        { id: '月社キサキ', roles: ['proof'], x: 41.39, y: 56.25 },
        { id: 'bouquet', roles: ['polish'], x: 89.41, y: 81.83 },
        { id: '朝日紫', roles: ['polish'], x: 88, y: 60 },
        { id: '白薇啤酒', roles: ['test'], x: 8.71, y: 83.14 },
        { id: '雪月涯', roles: ['test'], x: 23.66, y: 86.48 },
        { id: '君君子兰', roles: ['test'], x: 36, y: 82 },
        { id: '稻田养生', roles: ['test'], x: 68.42, y: 85.90 },
        { id: '久岛理', roles: ['test'], x: 74.06, y: 76.02 },
        { id: 'weii', roles: ['test'], x: 55.84, y: 81.40 },
        { id: 'special', label: '致谢前汉化组', roles: ['special'], x: 49.51, y: 92.88 }
];

const LINKS = [
    ['鳥谷真琴厨', '喵喵酱'],
    ['DevSeeD', '这位同学'],
    ['喵喵酱', '这位同学'],
    ['这位同学', '百分百原味胖次'],
    ['喵喵酱', '百分百原味胖次'],
    ['喵喵酱', '橘猫'],
    ['喵喵酱', '情报弱者'],
    ['喵喵酱', 'バカ'],
    ['喵喵酱', 'ニャ森'],
    ['喵喵酱', '小白'],
    ['这位同学', 'Ayachi00'],
    ['这位同学', '绅士君'],
    ['这位同学', '星洲鯨鯊。'],
    ['这位同学', 'なずな'],
    ['百分百原味胖次', '森凉℃'],
    ['小白', '夜寒'],
    ['小白', '月社キサキ'],
    ['ニャ森', 'nayuta'],
    ['ニャ森', 'ダレソカレ'],
    ['Ayachi00', 'bouquet'],
    ['绅士君', '朝日紫'],
    ['なずな', '稻田养生'],
    ['なずな', '久岛理'],
    ['森凉℃', '君君子兰'],
    ['森凉℃', 'weii'],
    ['夜寒', '雪月涯'],
    ['ダレソカレ', '白薇啤酒'],
    ['君君子兰', 'special'],
    ['稻田养生', 'special']
];

// CSV 数据
document.addEventListener('DOMContentLoaded', async () => {
    const isMobileUa = /Android|iPhone|iPad|iPod|Windows Phone/i.test(navigator.userAgent);
    if (isMobileUa) {
        alert('请在电脑端访问，体验更佳哦~');
        const viewport = document.querySelector('meta[name="viewport"]');
        if (viewport) {
            viewport.setAttribute('content', 'width=1280, initial-scale=1.0');
        }
        document.body.classList.add('force-desktop');
        document.body.classList.add('force-landscape');
    }

    // DOM 元素引用
    const viewMap = document.getElementById('viewMap');
    const viewDialogue = document.getElementById('viewDialogue');
    const bgOverlay = document.querySelector('.bg-overlay');
    const roleMenu = document.getElementById('roleMenu');
    const linesLayer = document.getElementById('linesLayer');
    const nodesLayer = document.getElementById('nodesLayer');
    const chartMapArea = document.querySelector('.chart-map');
    const container = document.querySelector('.container');

    const hoverRoles = document.getElementById('hoverRoles');
    const hoverTitle = document.getElementById('hoverTitle');
    const hoverDesc = document.getElementById('hoverDesc');
    const dialogBox = document.getElementById('dialogBox');
    const dialogName = document.getElementById('dialogName');
    const dialogText = document.getElementById('dialogText');
    const charStand = document.getElementById('charStand');
    const dialogArt = document.getElementById('dialogArt');
    const dialogArtImg = document.getElementById('dialogArtImg');

    let currentDialogLines = [];
    let currentLineIndex = 0;
    const supportsPointer = 'PointerEvent' in window;

    // --- CSV 解析 ---
    function parseCSVString(csvText) {
        const rows = csvText.split(/\r?\n/).map(line => line.trim()).filter(line => line);
        const dataMap = new Map();
        for (let i = 1; i < rows.length; i++) {
            const match = rows[i].match(/([^,]+),([^,]*),([^,]*),(.*)/);
            if (match) {
                const key = match[1].trim();
                let content = match[4].trim();
                if (content.startsWith('"') && content.endsWith('"')) content = content.slice(1, -1);
                dataMap.set(key, { avatar: match[2].trim(), ep: match[3].trim(), content });
            }
        }
        return dataMap;
    }
    const csvData = parseCSVString(csvContent);
    hoverTitle.innerText = "汉化感言";

    // --- 核心：连线重绘函数 ---
    function drawLines() {
        linesLayer.innerHTML = '';
        LINKS.forEach(link => {
            const [startId, endId] = link;
            const sNode = NODES.find(n => n.id === startId || n.label === startId);
            const eNode = NODES.find(n => n.id === endId || n.label === endId);

            if (sNode && eNode) {
                const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line.setAttribute('x1', sNode.x + '%');
                line.setAttribute('y1', sNode.y + '%');
                line.setAttribute('x2', eNode.x + '%');
                line.setAttribute('y2', eNode.y + '%');
                line.setAttribute('stroke', '#6fb6e8');
                line.setAttribute('stroke-width', '2');
                line.setAttribute('opacity', '0.65');
                linesLayer.appendChild(line);
            }
        });
    }

    // --- 1. 生成菜单 ---
    ROLES.forEach(role => {
        const btn = document.createElement('div');
        btn.className = `role-btn ${role.style || ''}`;
        btn.innerText = role.label;
        btn.addEventListener('click', () => {
            document.querySelectorAll('.role-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            document.querySelectorAll('.node-btn').forEach(nodeBtn => {
                const nodeRoles = JSON.parse(nodeBtn.dataset.roles || "[]");
                if (role.id === 'all') {
                    nodeBtn.classList.remove('dimmed', 'highlight');
                } else {
                    if (nodeRoles.includes(role.id)) {
                        nodeBtn.classList.remove('dimmed');
                        nodeBtn.classList.add('highlight');
                    } else {
                        nodeBtn.classList.remove('highlight');
                        nodeBtn.classList.add('dimmed');
                    }
                }
            });
        });
        roleMenu.appendChild(btn);
    });
    roleMenu.firstElementChild?.click();

    // --- 2. 绘制节点 & 拖拽逻辑 ---
    let isDragging = false;
    let activeDragNode = null;

    function getOffsetToContainer(el) {
        let x = 0;
        let y = 0;
        let current = el;
        while (current && current !== container) {
            x += current.offsetLeft || 0;
            y += current.offsetTop || 0;
            current = current.offsetParent;
        }
        return { x, y };
    }

    function getDragPoint(clientX, clientY) {
        if (!document.body.classList.contains('force-landscape')) {
            const rect = chartMapArea.getBoundingClientRect();
            return { x: clientX - rect.left, y: clientY - rect.top };
        }
        const rect = container.getBoundingClientRect();
        const sx = clientX - rect.left;
        const sy = clientY - rect.top;
        const containerHeight = container.offsetHeight;
        const localX = sy;
        const localY = containerHeight - sx;
        const offset = getOffsetToContainer(chartMapArea);
        return { x: localX - offset.x, y: localY - offset.y };
    }

    NODES.forEach(node => {
        const btn = document.createElement('div');
        btn.className = 'node-btn';
        const lookupKey = node.label || node.id;
        const nodeData = csvData.get(lookupKey);

        node.runtimeEp = nodeData ? nodeData.ep : "测试ep文本";
        node.runtimeContent = nodeData ? nodeData.content : "";
        node.runtimeAvatar = nodeData ? nodeData.avatar : "";

        btn.innerText = lookupKey;
        btn.style.left = node.x + '%';
        btn.style.top = node.y + '%';
        btn.dataset.roles = JSON.stringify(node.roles);
        if(node.roles.includes('special')) btn.classList.add('node-special');

        // Hover
        btn.addEventListener('mouseenter', () => {
            if (!isDragging) {
                hoverTitle.innerText = lookupKey;
                hoverDesc.innerText = node.runtimeEp;
                const roleLabels = node.roles
                    .filter(r => r !== 'special')
                    .map(rId => {
                        const r = ROLES.find(item => item.id === rId);
                        return r ? r.label : '';
                    })
                    .filter(l => l).join(' / ');
                hoverRoles.innerText = roleLabels || (node.roles.includes('special') ? '特别致谢' : '');
                btn.classList.add('hovering');
            }
        });
        btn.addEventListener('mouseleave', () => {
            btn.classList.remove('hovering');
        });

        // 鼠标/触摸按下：准备拖拽
        if (supportsPointer) {
            btn.addEventListener('pointerdown', (e) => {
                if (e.pointerType === 'mouse' && e.button !== 0) return;
                isDragging = false;
                const start = getDragPoint(e.clientX, e.clientY);
                activeDragNode = {
                    node,
                    btn,
                    startX: start.x,
                    startY: start.y,
                    pointerId: e.pointerId,
                    pointerType: e.pointerType
                };
                if (btn.setPointerCapture) {
                    try {
                        btn.setPointerCapture(e.pointerId);
                    } catch (_) {}
                }
                document.addEventListener('pointermove', onPointerMove);
                document.addEventListener('pointerup', onPointerUp);
                document.addEventListener('pointercancel', onPointerUp);
            });
        } else {
            btn.addEventListener('mousedown', (e) => {
                if (e.button !== 0) return;
                isDragging = false;
                const start = getDragPoint(e.clientX, e.clientY);
                activeDragNode = { node, btn, startX: start.x, startY: start.y, pointerId: null, pointerType: 'mouse' };
                document.addEventListener('mousemove', onMouseMove);
                document.addEventListener('mouseup', onMouseUp);
            });
        }

        // 点击事件 (如果是拖拽则不触发)
        btn.addEventListener('click', (e) => {
            if (!isDragging) {
                enterDialogueMode(node, lookupKey);
            }
        });

        nodesLayer.appendChild(btn);
    });

    // 初始绘制连线
    drawLines();

    // 拖拽处理
    function handleDragMove(clientX, clientY) {
        if (!activeDragNode) return;
        const point = getDragPoint(clientX, clientY);
        const dx = point.x - activeDragNode.startX;
        const dy = point.y - activeDragNode.startY;
        // 移动超过3像素才算拖拽，防止误触点击
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
            isDragging = true;
        }

        if (isDragging) {
            const baseWidth = chartMapArea.offsetWidth;
            const baseHeight = chartMapArea.offsetHeight;
            if (!baseWidth || !baseHeight) return;
            // 像素转百分比
            let newX = (point.x / baseWidth) * 100;
            let newY = (point.y / baseHeight) * 100;
            // 限制边界
            newX = Math.max(0, Math.min(100, newX));
            newY = Math.max(0, Math.min(100, newY));

            // 更新数据
            activeDragNode.node.x = newX;
            activeDragNode.node.y = newY;
            // 更新DOM
            activeDragNode.btn.style.left = newX + '%';
            activeDragNode.btn.style.top = newY + '%';
            // 重绘连线
            drawLines();
        }
    }

    function onPointerMove(e) {
        if (!activeDragNode) return;
        if (activeDragNode.pointerId !== null && e.pointerId !== activeDragNode.pointerId) return;
        if (activeDragNode.pointerType === 'touch' && isDragging) {
            e.preventDefault();
        }
        handleDragMove(e.clientX, e.clientY);
    }

    function onPointerUp(e) {
        if (!activeDragNode) return;
        if (activeDragNode.pointerId !== null && e.pointerId !== activeDragNode.pointerId) return;
        activeDragNode = null;
        document.removeEventListener('pointermove', onPointerMove);
        document.removeEventListener('pointerup', onPointerUp);
        document.removeEventListener('pointercancel', onPointerUp);
        setTimeout(() => { isDragging = false; }, 0);
    }

    function onMouseMove(e) {
        handleDragMove(e.clientX, e.clientY);
    }

    function onMouseUp() {
        activeDragNode = null;
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
        setTimeout(() => { isDragging = false; }, 0);
    }

    function positionAvatar() {
        if (viewDialogue.classList.contains('hidden')) return;
        if (charStand.style.display === 'none') return;
        const boxRect = dialogBox.getBoundingClientRect();
        const tagRect = dialogName.getBoundingClientRect();
        const centerX = tagRect.left - boxRect.left + tagRect.width / 2;
        const topY = tagRect.top - boxRect.top;
        charStand.style.left = `${centerX}px`;
        charStand.style.top = `${topY}px`;
    }

    window.addEventListener('resize', () => {
        if (!viewDialogue.classList.contains('hidden')) {
            positionAvatar();
        }
    });

    dialogArt.addEventListener('click', (e) => {
        e.stopPropagation();
        if (dialogArt.classList.contains('hidden')) return;
        dialogArt.classList.toggle('zoomed');
    });

    // --- AVG 系统 ---
    function enterDialogueMode(node, name) {
        const isEaster = node.id === 'egg';
        let rawContent = node.runtimeContent;
        if (!rawContent) {
             // 彩蛋默认文本
             if (node.id === 'egg') rawContent = "🎉🎉🎉🎉🎉";
             else rawContent = "暂无内容...@p请配置文本。";
        }

        currentDialogLines = rawContent.split('@p').map(page => page.replace(/@n/g, '<br>'));
        currentLineIndex = 0;
        dialogName.innerText = name;

        dialogArt.classList.add('hidden');
        dialogArt.classList.remove('zoomed');
        dialogArtImg.removeAttribute('src');
        dialogArtImg.alt = '';

        charStand.innerHTML = '';
        charStand.style.display = 'none';
        const avatarRaw = node.runtimeAvatar ? node.runtimeAvatar.trim() : '';
        const avatarKey = avatarRaw !== '' ? avatarRaw : '默认头像.webp';
        const avatarUrl = avatarMap.get(avatarKey) || `images/head/${avatarKey}`;
        if (avatarKey) {
            const img = document.createElement('img');
            img.src = avatarUrl;
            img.alt = name;
            img.style.display = 'block';
            img.onerror = function() { this.style.display = 'none'; };
            charStand.appendChild(img);
            charStand.style.display = 'block';
        }

        if (isEaster) {
            dialogArtImg.src = easterArtUrl;
            dialogArtImg.alt = name;
            dialogArt.classList.remove('hidden');
        }

        nodesLayer.classList.add('dialogue-active');
        if (isEaster) {
            document.body.classList.add('easter-active');
        }
        viewDialogue.classList.remove('hidden');
        requestAnimationFrame(positionAvatar);
        showNextLine();
    }

    function exitDialogueMode() {
        viewDialogue.classList.add('hidden');
        nodesLayer.classList.remove('dialogue-active');
        currentDialogLines = [];
        currentLineIndex = 0;
        charStand.innerHTML = '';
        dialogArt.classList.add('hidden');
        dialogArt.classList.remove('zoomed');
        dialogArtImg.removeAttribute('src');
        dialogArtImg.alt = '';
    }

    function showNextLine() {
        if (currentLineIndex >= currentDialogLines.length) {
            exitDialogueMode();
            return;
        }
        dialogText.innerHTML = currentDialogLines[currentLineIndex];
        currentLineIndex++;
    }

    dialogBox.addEventListener('click', (e) => { e.stopPropagation(); showNextLine(); });
    viewDialogue.addEventListener('click', () => { showNextLine(); });
});