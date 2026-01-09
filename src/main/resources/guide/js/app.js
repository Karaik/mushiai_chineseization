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
        { id: 'shit', label: '💩💩💩', roles: ['shit'], x: 52, y: 38 },
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

// 内联 CSV 数据
const csvContent = `Key,Avatar,Episode,Content
鳥谷真琴厨,鳥谷真琴厨.png,因为要求太高导致大部分文本重翻的监修,记得22年的某一天，喜欢的女生对我说，她很想玩虫爱，但没有汉化。@n恐怕就是因为这几句话，我才会在半年后忽悠夕里和喵喵酱开虫爱。@n虽然开坑之后，我就跟她断了联系，@n夕里也很快就跑路了，整个24年基本没有进度。@p但好在喵喵酱没有放弃虫爱，所以才有现在的合作，@n跟甜心组一起填完坑……真的非常感谢喵喵酱，@n如果没有他的辛勤付出，虫爱依然会是万年大坑吧。@p（私货一则：请来看我们百合神作《ふつおたはいりません!》 ）
喵喵酱,喵喵酱.png,反正就是给所有人都擦了个屁股的卫生纸,大家好，我是接了这个虫子游戏然后冲到肾虚的喵喵酱。@n《虫爱少女》这部上古猎奇作品经历了15年汉化终于圆满了。@n为了呈现出更好汉化效果，这部作品的好多岗位都换了好多次x@n留下来的勇士也几乎都是身兼数职，感谢感谢。@p说到这部作品，我还是最喜欢【溶于森林】结局（END20），@n打完这个结局后久久不能平静，也希望大家来探索~@n本作有三十多个结局，不看攻略几乎不太可能全CG（欢迎探索），@n因此我在补丁里附上了全攻略和全cg存档，方便大家直接游玩！@p好了，我要说的就这么多了。@n最后，《虫爱少女》有7个FD也将开始汉化，@n希望各位能助甜心组一臂之力！@p（这段感言模仿了BONUS TRACK和泉万夜的文风x）
DevSeeD,DevSeeD.png,无时无刻都秒回且秒出补丁的编译机器人,systemnnn引擎的游戏， 由于之前分析过且写好了自动化构建脚本，@n这个游戏就可以很快迁移过来了。@p这个游戏的难点主要是原来的文本格式非常不规范， 句子之间还有错位，@n前期迁移费了番功夫，还有一些犄角旮旯的文本需要处理。@p这次汉化整体来说进行还算顺利， 听说之前经过了好几个汉化组，@n终于要完成了~ 祝大家玩的开心。
这位同学,这位同学.png,好像是个做程序的但怎么感觉什么都做了,这是本人参与汉化的第二部作品，对我而言也意义非凡。@n这一次算是把汉化过程中该走的流程都完整体验了一遍，@n终于敢说自己能单开坑了。@p喵喵把非常关键的真结局，以及最后的战斗部分交给我负责。@n看到是这么重要的场景，我也拿出了 120% 的精力，@n希望大家能在后半段剧情里沉浸到底。@n（已燃尽……我记得我好像只是个程序来着？）@p如果你在游玩时不会因为文本的措辞与表达感到困惑，@n且能沉浸到故事中，那就是对我们最大的支持。@n希望你能玩得开心！
百分百原味胖次,百分百原味胖次.png,这姑娘为啥校着校着就变成了修图大佬了,大家好啊，很荣幸能参与《虫爱少女》这部游戏的汉化。@n这是我参与汉化的第四部魔器，也是越来越熟练了（笑）。@n也祝大家冲得开心！反正我是重云了~@p（这边依旧是为了让感言过审，写得非常没有意思的百分百原味胖次）@p（喵喵酱：真的没有意思吗？或许各位可以探索下这个人附近的东西）
橘猫,橘猫.png,了解这部作品陈年往事的超级老前辈翻译,好像负责了很多东西，但感觉好像又什么东西都没干，基本纯摆烂。
情报弱者,情报弱者.png,帮忙擦了个屁股的翻译，也不知道吃了没,少见的剧情拔，建议各位在冲的时候也看一眼HS文本，@n有不少对人物的塑造。@n另外几年前我还在等虫爱汉化，没想到现在自己也能参与进来，真的很高兴。
バカ,バカ.png,帮忙吃了个答辩的翻译，也不知道擦了没,刚入坑就听闻魔器的大名了，自己上手才感觉这是被名号耽误了的@n宝藏佳作，都来玩喵
小白,小白.png,因为通宵沉迷汉化导致挂科的鬼畜大学生,
星洲鯨鯊。,星洲鯨鯊。.png,连通勤都在汉化的兄弟，不怕社死的真神,你是谁？请支持虫爱少女！
Ayachi00,Ayachi00.png,有求必应的翻译兼润色，不给粮食都拉磨,翻译的时候感觉自己已经被触手薅了无数遍，到最后无感了。@n其实猎奇倒也没有特别猎奇吧，也可能我的身心已经很强大了喵）@n快要毕业了希望修考一切顺利，最后的最后，我永远喜欢绫地宁宁～
绅士君,绅士君.png,有求必应的润色兼翻译，但拉磨给了粮食,虫爱少女简直就是一个乙游大世界💕@p🖤想要孔武有力，阳光开朗有炼悟🖤，@n💛想要心思缜密运筹帷幄有西💛，@n❤️想要成熟稳重有莲❤️，@n💜如果想要我们帅气的男主有优斗💜，@p💗要我说，你们虫爱玩家吃的也真是太好了吧💘，@n🎁今天我已经垂直入坑了🎉
ニャ森,"ニャ森,png",无需多言的汉化届大神，不知道咋撬来的,本质上虫爱是个主打“比我拔的没我剧情好，比我剧情好的没我拔”的游戏。剧情虽不算精彩，但胜在完整度高。@n拔这一块不算特别猎奇，但胜在各种play量大管饱。@p（顺便说一句，应该没有人打完之后不想养一条虫虫吧）
森凉℃,森凉℃.png,恐怕能二十四小时连轴转的汉化唯一真神,多的不说，只愿各位能够沉浸其中，尽享其乐~
nayuta,nayuta.png,临危受命时苟利事业生死以的名誉老校对,很荣幸能参与这部久仰大名的佳作，给老文本全部翻新的各位苦力真的不容易
夜寒,夜寒.png,这位红脸长须校对好像是某贴吧的吧主吧,
ダレソカレ,ダレソカレ.png,临危受命时岂因祸福避趋之的新鲜小校对,
月社キサキ,月社キサキ.png,隔壁汉化组被无情挖来往死里干活的大神,没帮什么忙，各位辛苦了
なずな,なずな.png,一半润色是这妹妹干的，出错了骂她就好,关注永雏塔菲喵，关注永雏塔菲谢谢喵@p（美弥香的白丝真棒）
bouquet,bouquet.png,只负责监修校对的文本，把监修骂死那种,
朝日紫,朝日紫.png,做事一丝不苟刨根问底的认真铁血真男儿,参加的第二部作品😌所以我到底是为什么蹚了这趟浑水？？？*^_^*@n每次就这样被百合骗进一趟趟贼船然后发现自己负责的部分并没有百合@n（残念）
白薇啤酒,白薇啤酒.png,要不是他测试提出几千条问题早就发布了,本来只是在跟喵喵聊天，结果聊着聊着他突然掏出麻袋套我头上，@n就把我绑到这艘贼船上了呜呜。@p而我也没想到自己居然会有参与虫爱汉化的一天，@n没想到自己会被梦美这个女生所吸引，@n在观看故事时不止一次揪着心期待能看到梦美获得幸福的样子。@p尽管有不少残酷的情节，但若能看到梦美幸福，就都无所谓了。@n负责本作剧本的和泉老师在文笔上尚显疏涩，给汉化带来了不少挑战，@n但大家还是很给力的@p（最后希望自己能撑到隔壁的樱之响面世再退休）
雪月涯,雪月涯.png,测试时过于投入爱上剧情边冲边测试的屑,很不错的作品，苦于画风比较古老，有点难冲，但还是出来了。
君君子兰,君君子兰.png,测试时过于认真精神错乱拜史郎为师的渣,
稻田养生,稻田养生.png,绝不缺席任何一次猎奇游戏测试工作的神,
久岛理,久岛理.png,绝不缺席任何一次虫子游戏测试工作的仙,在入坑那会就非常期待虫爱的剧情，在能参加测试之后玩的非常开心，也非常满意自己的黑c补全进度更进一步。爱你们呦。
weii,weii.png,半路杀出来的强悍测试，仨月更比两年强,去年十月，@n可爱的百分百原味胖次酱问我要不要来测《EXTRAVAGANZA》。@n于是本BLACK Cyc小子就这样被拉入坑了……@p其实《EXTRAVAGANZA》是我初中时接触的作品，@n很荣幸参与这次汉化工作，也算给我的青春划上了句点。@n第一次参与那么长篇幅gal的测试，非常感谢大佬们指点迷津。@p题外话，好想养一只虫虫啊。@n谁会拒绝一只黏人但不掉毛的虫虫呢，简直是理想宠物（误）。
致谢前汉化组,致谢前汉化组.png,感谢各位前辈十余年来为虫爱汉化的贡献,yuugiri、兔耳茶、琉璃、2l模仿47哥、借光、溪蒼、ty清雲@n长冷、查士丁尼、水产罐头、r514783、shirayuki noa、未確認、夕里
💩💩💩,💩💩💩.png,这是一张因为太怪惨遭喵喵酱毙掉的贺图,哦呀，背景图好像变化了呢~@n（点击中央图片可放大查看）`;

document.addEventListener('DOMContentLoaded', async () => {
    // DOM 元素引用
    const viewMap = document.getElementById('viewMap');
    const viewDialogue = document.getElementById('viewDialogue');
    const bgOverlay = document.querySelector('.bg-overlay');
    const roleMenu = document.getElementById('roleMenu');
    const linesLayer = document.getElementById('linesLayer');
    const nodesLayer = document.getElementById('nodesLayer');
    const chartMapArea = document.querySelector('.chart-map');

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

    // --- CSV 解析 ---
    function parseCSVString(csvText) {
        const rows = csvText.split('\n').map(line => line.trim()).filter(line => line);
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

        // 鼠标按下：准备拖拽
        btn.addEventListener('mousedown', (e) => {
            isDragging = false;
            activeDragNode = { node, btn, startX: e.clientX, startY: e.clientY };
            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
        });

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
    function onMouseMove(e) {
        if (!activeDragNode) return;
        const dx = e.clientX - activeDragNode.startX;
        const dy = e.clientY - activeDragNode.startY;
        // 移动超过3像素才算拖拽，防止误触点击
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
            isDragging = true;
        }

        if (isDragging) {
            const rect = chartMapArea.getBoundingClientRect();
            // 像素转百分比
            let newX = ((e.clientX - rect.left) / rect.width) * 100;
            let newY = ((e.clientY - rect.top) / rect.height) * 100;
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
        const isEaster = node.id === 'shit';
        let rawContent = node.runtimeContent;
        if (!rawContent) {
             // 彩蛋默认文本
             if (node.id === 'shit') rawContent = "💩💩💩💩💩";
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
        if (node.runtimeAvatar && node.runtimeAvatar.trim() !== '') {
            const img = document.createElement('img');
            img.src = `images/head/${node.runtimeAvatar}`;
            img.alt = name;
            img.style.display = 'block';
            img.onerror = function() { this.style.display = 'none'; };
            charStand.appendChild(img);
            charStand.style.display = 'block';
        }

        if (isEaster) {
            dialogArtImg.src = 'images/egg.png';
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