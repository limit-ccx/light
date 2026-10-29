<template>
  <div class="kg-page">
    <Header />
    <main class="main-content">
      <div class="page-container">
        <!-- 页面标题 -->
        <div class="page-header">
          <div class="title-area">
            <span class="title-eyebrow"><i></i>Knowledge Constellation</span>
            <h1>
              <el-icon class="title-glyph"><MostlyCloudy /></el-icon>
              <span class="gradient-title">知识星图</span>
            </h1>
            <p class="title-sub">在银河星海中漫游，探索知识星球之间的引力联结</p>
            <div class="title-stardust" aria-hidden="true">
              <i></i><i></i><i></i><i></i><i></i>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="primary" :icon="Refresh" @click="loadGraphData" :loading="loading" class="galaxy-btn">
              刷新图谱
            </el-button>
            <el-button :icon="QuestionFilled" @click="showHelp = true" class="galaxy-btn-plain">帮助</el-button>
          </div>
        </div>

        <div class="workspace">
          <!-- 左侧统计面板 -->
          <aside class="stats-panel">
            <div class="panel-card nebula-card">
              <h3>
                <el-icon><View /></el-icon>
                星系概览
              </h3>
              <div class="stat-item">
                <span class="stat-label">
                  <el-icon><MostlyCloudy /></el-icon> 星球总数
                </span>
                <span class="stat-value glow-number">{{ stats.totalNodes || 0 }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">
                  <el-icon><Connection /></el-icon> 引力连线
                </span>
                <span class="stat-value glow-number cyan">{{ stats.totalEdges || 0 }}</span>
              </div>
              <div class="stat-item" v-if="stats.totalNodes">
                <span class="stat-label">
                  <el-icon><View /></el-icon> 可见星球
                </span>
                <span class="stat-value glow-number amber">{{ displayedNodeCount }} / {{ stats.totalNodes }}</span>
              </div>
            </div>

            <div class="panel-card nebula-card">
              <h3>
                <el-icon><Link /></el-icon>
                星球类型
              </h3>
              <div class="type-legend">
                <div
                  v-for="(count, type) in stats.nodesByType"
                  :key="type"
                  class="type-item"
                  :class="{ active: selectedType === type }"
                  @click="toggleTypeFilter(type)"
                >
                  <span class="type-dot" :style="{ background: getTypeColor(type), boxShadow: `0 0 10px ${getTypeColor(type)}` }"></span>
                  <span class="type-name">{{ getTypeLabel(type) }}</span>
                  <span class="type-count">{{ count }}</span>
                </div>
              </div>
              <button v-if="selectedType" type="button" class="clear-type" @click="toggleTypeFilter(selectedType)">
                清除类型筛选
              </button>
            </div>

            <div class="panel-card nebula-card hint-card">
              <el-icon class="hint-icon"><InfoFilled /></el-icon>
              <p>拖拽节点可调整位置，滚轮缩放画布，点击节点查看详情。</p>
            </div>
          </aside>

          <!-- 主图谱区域 -->
          <div class="graph-area">
            <StarField class="star-field-bg" :star-count="380" :shooting-star-interval="1800" />
            <div class="nebula-belt belt-1" aria-hidden="true"></div>
            <div class="nebula-belt belt-2" aria-hidden="true"></div>
            <div class="nebula-cloud cloud-1" aria-hidden="true"></div>
            <div class="nebula-cloud cloud-2" aria-hidden="true"></div>
            <div class="galaxy-core" aria-hidden="true"></div>
            <div class="galaxy-aurora aurora-a" aria-hidden="true"></div>
            <div class="galaxy-aurora aurora-b" aria-hidden="true"></div>
            <div class="graph-rings" aria-hidden="true">
              <span class="ring-1"></span>
              <span class="ring-2"></span>
              <span class="ring-3"></span>
            </div>

            <!-- 工具栏 -->
            <div class="toolbar">
              <div class="search-wrap">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索节点名称..."
                  :prefix-icon="Search"
                  clearable
                  @keyup.enter="handleSearch"
                  @clear="clearSearch"
                />
                <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
              </div>
              <div class="tool-actions">
                <el-tooltip content="适应屏幕">
                  <el-button :icon="FullScreen" circle @click="fitGraph" />
                </el-tooltip>
                <el-tooltip content="重置视图">
                  <el-button :icon="RefreshRight" circle @click="resetView" />
                </el-tooltip>
                <el-tooltip content="切换布局">
                  <el-button :icon="Switch" circle @click="toggleLayout" />
                </el-tooltip>
                <el-divider direction="vertical" />
                <el-button :icon="Plus" @click="openAddNode">添加节点</el-button>
                <el-button :icon="Share" @click="openAddEdge">添加关系</el-button>
              </div>
            </div>

            <!-- 图表容器 -->
            <div class="chart-wrapper">
              <div ref="chartRef" class="chart-container"></div>

              <div v-if="loading" class="galaxy-loading">
                <span class="gl-orbit">
                  <i></i><i></i><i></i>
                  <b></b>
                </span>
                <p>正在绘制星系…</p>
              </div>

              <!-- 空状态 -->
              <div v-if="!loading && rawNodes.length === 0" class="empty-state">
                <el-empty description="暂无知识图谱数据">
                  <template #image>
                    <div class="empty-icon"><el-icon><Share /></el-icon></div>
                  </template>
                  <p class="empty-hint">
                    知识图谱可以从文章中自动提取，也可以手动添加节点和关系。
                  </p>
                  <div class="empty-actions">
                    <el-button type="primary" @click="goToKnowledge">去知识库导入文章</el-button>
                    <el-button @click="openAddNode">手动添加节点</el-button>
                  </div>
                </el-empty>
              </div>

              <div class="graph-vignette" aria-hidden="true"></div>

              <div class="graph-hud" aria-hidden="true">
                <span class="hud-dot"></span>
                <b>{{ displayedNodeCount }}</b> 个星球
                <span class="hud-sep"></span>
                <b>{{ stats.totalEdges || 0 }}</b> 条引力
                <span class="hud-sep"></span>
                拖拽 · 滚轮缩放 · 点击查看
              </div>
            </div>
          </div>

          <!-- 右侧详情面板 -->
          <aside class="detail-panel nebula-panel" :class="{ active: selectedNode }">
            <template v-if="selectedNode">
              <div class="panel-header" :style="{ '--node-accent': getTypeColor(selectedNode.type) }">
                <div class="node-title">
                  <span class="node-orb" :style="nodeOrbStyle(selectedNode.type)"></span>
                  <h3>{{ selectedNode.name }}</h3>
                </div>
                <el-button text :icon="Close" circle @click="clearSelection" />
              </div>

              <div class="panel-body">
                <div class="info-row">
                  <span class="label">类型</span>
                  <el-tag :color="getTypeColor(selectedNode.type)" effect="dark">
                    {{ getTypeLabel(selectedNode.type) }}
                  </el-tag>
                </div>
                <div class="info-row">
                  <span class="label">ID</span>
                  <span class="value">{{ selectedNode.id }}</span>
                </div>
                <div class="info-row">
                  <span class="label">引力连接</span>
                  <span class="value">{{ outgoingEdges.length + incomingEdges.length }}</span>
                </div>

                <div class="relations-section">
                  <div class="section-title">
                    <span>出边关系</span>
                    <el-tag size="small">{{ outgoingEdges.length }}</el-tag>
                  </div>
                  <div v-if="outgoingEdges.length === 0" class="empty-text">无出边</div>
                  <div v-else class="relation-list">
                    <div
                      v-for="edge in outgoingEdges"
                      :key="edge.id"
                      class="relation-item"
                      @click="selectNodeById(edge.targetId)"
                    >
                      <span class="relation-name">{{ edge.relation }}</span>
                      <el-icon><Right /></el-icon>
                      <span class="node-name">{{ edge.targetName }}</span>
                    </div>
                  </div>
                </div>

                <div class="relations-section">
                  <div class="section-title">
                    <span>入边关系</span>
                    <el-tag size="small">{{ incomingEdges.length }}</el-tag>
                  </div>
                  <div v-if="incomingEdges.length === 0" class="empty-text">无入边</div>
                  <div v-else class="relation-list">
                    <div
                      v-for="edge in incomingEdges"
                      :key="edge.id"
                      class="relation-item"
                      @click="selectNodeById(edge.sourceId)"
                    >
                      <span class="node-name">{{ edge.sourceName }}</span>
                      <el-icon><Right /></el-icon>
                      <span class="relation-name">{{ edge.relation }}</span>
                    </div>
                  </div>
                </div>

                <div class="panel-actions">
                  <el-button type="primary" @click="openAddEdgeWithSource(selectedNode)">
                    添加关系
                  </el-button>
                  <el-button type="danger" plain @click="deleteSelectedNode">删除节点</el-button>
                </div>
              </div>
            </template>

            <div v-else class="no-selection">
              <div class="empty-orbit" aria-hidden="true">
                <span class="eo-ring eo-ring-1"></span>
                <span class="eo-ring eo-ring-2"></span>
                <span class="eo-core"><el-icon><Pointer /></el-icon></span>
              </div>
              <h4>选择一个星球</h4>
              <p>点击图谱中的节点，查看类型、引力关系与节点操作。</p>
              <div class="no-selection-tips">
                <span>拖拽调整</span>
                <span>滚轮缩放</span>
                <span>双击聚焦</span>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </main>

    <!-- 添加节点对话框 -->
    <el-dialog v-model="showAddNodeDialog" title="添加节点" width="420px" destroy-on-close>
      <el-form :model="nodeForm" label-width="80px" @submit.prevent="addNode">
        <el-form-item label="节点名称" required>
          <el-input v-model="nodeForm.name" placeholder="请输入节点名称" />
        </el-form-item>
        <el-form-item label="节点类型" required>
          <el-select v-model="nodeForm.type" placeholder="选择类型" style="width: 100%">
            <el-option label="概念" value="concept" />
            <el-option label="人物" value="person" />
            <el-option label="技术栈" value="tech" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddNodeDialog = false">取消</el-button>
        <el-button type="primary" @click="addNode" :loading="saving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加关系对话框 -->
    <el-dialog v-model="showAddEdgeDialog" title="添加关系" width="460px" destroy-on-close>
      <el-form :model="edgeForm" label-width="90px" @submit.prevent="addEdge">
        <el-form-item label="源节点" required>
          <el-select-v2
            v-model="edgeForm.sourceId"
            :options="nodeOptions"
            placeholder="选择源节点"
            filterable
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="目标节点" required>
          <el-select-v2
            v-model="edgeForm.targetId"
            :options="nodeOptions"
            placeholder="选择目标节点"
            filterable
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="关系类型" required>
          <el-input v-model="edgeForm.relation" placeholder="例如：使用、包含、属于" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddEdgeDialog = false">取消</el-button>
        <el-button type="primary" @click="addEdge" :loading="saving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 帮助抽屉 -->
    <el-drawer v-model="showHelp" title="知识图谱使用帮助" size="420px">
      <div class="help-content">
        <section>
          <h4>什么是知识图谱？</h4>
          <p>知识图谱将文章中的概念、人物、技术栈等实体提取为节点，并识别它们之间的关系，以可视化的方式呈现知识结构。</p>
        </section>
        <section>
          <h4>如何生成图谱？</h4>
          <ol>
            <li>进入<b>知识库</b>，导入或收藏文章</li>
            <li>在文章详情页点击<b>提取知识</b>按钮</li>
            <li>系统自动识别实体和关系并保存</li>
            <li>返回知识图谱页面查看可视化结果</li>
          </ol>
        </section>
        <section>
          <h4>常用操作</h4>
          <ul>
            <li><b>拖拽星球</b>：调整节点位置</li>
            <li><b>滚轮</b>：缩放星空画布</li>
            <li><b>点击星球</b>：查看详情和关联关系</li>
            <li><b>双击星球</b>：聚焦到该节点</li>
            <li><b>搜索</b>：快速定位星球</li>
          </ul>
        </section>
        <section>
          <h4>手动维护</h4>
          <p>如果自动提取结果不完整，可以点击"添加节点"和"添加关系"手动补充星系内容。</p>
        </section>
      </div>
    </el-drawer>

    <!-- 新手引导提示 -->
    <el-dialog v-model="showTour" title="欢迎来到星际知识图谱" width="500px" :show-close="false" align-center>
      <el-steps :active="tourStep" finish-status="success" simple>
        <el-step title="星系画布" />
        <el-step title="搜索星球" />
        <el-step title="详情面板" />
        <el-step title="操作工具" />
      </el-steps>
      <div class="tour-content">
        <p v-if="tourStep === 0">星球代表知识实体，引力场连线代表它们之间的关系。支持拖拽、缩放和点击交互。</p>
        <p v-if="tourStep === 1">使用顶部搜索框快速定位感兴趣的星球。</p>
        <p v-if="tourStep === 2">点击星球后，右侧面板会显示详细信息和引力关联。</p>
        <p v-if="tourStep === 3">你可以添加星球和引力关系，也可以调整视图布局。</p>
      </div>
      <template #footer>
        <el-button v-if="tourStep > 0" @click="tourStep--">上一步</el-button>
        <el-button type="primary" @click="nextTourStep">
          {{ tourStep === 3 ? '开始探索' : '下一步' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import Header from '@/components/Header.vue'
import StarField from '@/components/StarField.vue'
import {
  Search, Refresh, Plus, Share, FullScreen, RefreshRight, Switch,
  QuestionFilled, Close, Pointer, InfoFilled, Right, Connection, View, Link, MostlyCloudy
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getKnowledgeGraphData, getKnowledgeGraphStats, searchKnowledgeNodes,
  getKnowledgeNodeDetail, addKnowledgeNode, addKnowledgeEdge, deleteKnowledgeNode
} from '@/api/knowledge'

const router = useRouter()
const chartRef = ref(null)
let chartInstance = null

// 数据
const rawNodes = ref([])
const rawEdges = ref([])
const stats = ref({})
const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const selectedNode = ref(null)
const outgoingEdges = ref([])
const incomingEdges = ref([])
const selectedType = ref(null)
const displayedNodeCount = ref(0)
const currentLayout = ref('force')

// UI 状态
const showAddNodeDialog = ref(false)
const showAddEdgeDialog = ref(false)
const showHelp = ref(false)
const showTour = ref(false)
const tourStep = ref(0)

const nodeForm = ref({ name: '', type: 'concept' })
const edgeForm = ref({ sourceId: null, targetId: null, relation: '关联' })

const typeColors = {
  concept: '#7C6BFF',
  person: '#EC4899',
  tech: '#10B981',
  other: '#F59E0B',
  unknown: '#888888'
}

const typeLabels = {
  concept: '概念',
  person: '人物',
  tech: '技术栈',
  other: '其他',
  unknown: '未知'
}

const planetPalettes = {
  concept: { main: '#7C6BFF', glow: '#9D8FFF' },
  person:  { main: '#EC4899', glow: '#F472B6' },
  tech:    { main: '#10B981', glow: '#34D399' },
  other:   { main: '#F59E0B', glow: '#FBBF24' },
  unknown: { main: '#888888', glow: '#AAAAAA' }
}

// 星球自转动画状态
const planetRotations = ref(new Map())
let rotationTimer = null

const getTypeColor = (type) => typeColors[type] || typeColors.unknown
const getTypeLabel = (type) => typeLabels[type] || '未知'

// 颜色调整辅助函数
const hexToRgb = (hex) => {
  const num = parseInt(hex.replace('#', ''), 16)
  return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255 }
}
const rgbToHex = ({ r, g, b }) => {
  return '#' + [r, g, b].map(v => Math.max(0, Math.min(255, v)).toString(16).padStart(2, '0')).join('')
}
const adjustColor = (hex, amount) => {
  const rgb = hexToRgb(hex)
  return rgbToHex({
    r: rgb.r + amount,
    g: rgb.g + amount,
    b: rgb.b + amount
  })
}

// 生成星球 SVG 图标
const createPlanetSvg = (baseColor, hasRing = false, seed = 0) => {
  const lighter = adjustColor(baseColor, 66)
  const darker = adjustColor(baseColor, -58)
  const stroke1 = adjustColor(baseColor, 35)
  const stroke2 = adjustColor(baseColor, -25)
  const ringColor = adjustColor(baseColor, 45)
  const offset = (n) => ((seed * 13 + n * 29) % 9) - 4
  const b1 = offset(1)
  const b2 = offset(2)
  const b3 = offset(3)
  const spotX = 62 + offset(4)
  const spotY = 60 + offset(5)
  const moonX = 80 + offset(6)
  const moonY = 23 + offset(7)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 100 100">
    <defs>
      <radialGradient id="pg" cx="28%" cy="28%" r="80%">
        <stop offset="0%" stop-color="${lighter}"/>
        <stop offset="45%" stop-color="${baseColor}"/>
        <stop offset="100%" stop-color="${darker}"/>
      </radialGradient>
      <radialGradient id="pa" cx="50%" cy="50%" r="50%">
        <stop offset="62%" stop-color="${baseColor}" stop-opacity="0"/>
        <stop offset="86%" stop-color="${lighter}" stop-opacity="0.18"/>
        <stop offset="100%" stop-color="${baseColor}" stop-opacity="0"/>
      </radialGradient>
      <linearGradient id="rg" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="${lighter}" stop-opacity="0.9"/>
        <stop offset="55%" stop-color="${baseColor}" stop-opacity="0.55"/>
        <stop offset="100%" stop-color="${darker}" stop-opacity="0.8"/>
      </linearGradient>
      <filter id="pglow" x="-50%" y="-50%" width="200%" height="200%">
        <feGaussianBlur stdDeviation="3.8" result="blur"/>
        <feMerge>
          <feMergeNode in="blur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    <circle cx="50" cy="50" r="46" fill="url(#pa)"/>
    ${hasRing ? `
      <ellipse cx="50" cy="50" rx="52" ry="15.5" fill="none" stroke="url(#rg)" stroke-width="2.2" opacity="0.62" transform="rotate(-12 50 50)"/>
      <ellipse cx="50" cy="50" rx="44" ry="11" fill="none" stroke="${lighter}" stroke-width="0.8" stroke-dasharray="3 5" opacity="0.34" transform="rotate(16 50 50)"/>
      <circle cx="${moonX}" cy="${moonY}" r="4.2" fill="${lighter}" opacity="0.9"/>
    ` : ''}
    <circle cx="50" cy="50" r="36" fill="url(#pg)" filter="url(#pglow)"/>
    <circle cx="50" cy="50" r="35" fill="none" stroke="${lighter}" stroke-width="0.8" opacity="0.24"/>
    <path d="M 22,${38 + b1} Q 50,${28 + b1} 78,${40 + b1}" fill="none" stroke="${stroke1}" stroke-width="3" opacity="0.42" stroke-linecap="round"/>
    <path d="M 18,${54 + b2} Q 50,${44 + b2} 82,${56 + b2}" fill="none" stroke="${stroke2}" stroke-width="3" opacity="0.38" stroke-linecap="round"/>
    <path d="M 24,${70 + b3} Q 50,${60 + b3} 76,${72 + b3}" fill="none" stroke="${stroke1}" stroke-width="3" opacity="0.4" stroke-linecap="round"/>
    <ellipse cx="37" cy="29" rx="13" ry="6" fill="#ffffff" opacity="0.12" transform="rotate(-28 37 29)"/>
    <circle cx="34" cy="27" r="2.6" fill="#ffffff" opacity="0.26"/>
    <circle cx="${spotX}" cy="${spotY}" r="5" fill="${darker}" opacity="0.34"/>
    <circle cx="39" cy="39" r="3.4" fill="${lighter}" opacity="0.28"/>
  </svg>`
  return 'image://data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svg)))
}

const nodeOrbStyle = (type) => {
  const color = getTypeColor(type)
  return {
    background: `radial-gradient(circle at 32% 28%, #ffffff 0%, ${color} 42%, ${adjustColor(color, -52)} 100%)`,
    boxShadow: `0 0 18px ${color}, inset -3px -3px 8px rgba(0, 0, 0, 0.3)`
  }
}

const escapeHtml = (value) =>
  String(value ?? '').replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]))

// 计算节点度数
const computeNodeDegrees = () => {
  const degrees = new Map()
  rawNodes.value.forEach(n => degrees.set(n.id, 0))
  rawEdges.value.forEach(e => {
    degrees.set(e.sourceId, (degrees.get(e.sourceId) || 0) + 1)
    degrees.set(e.targetId, (degrees.get(e.targetId) || 0) + 1)
  })
  return degrees
}

const nodeOptions = computed(() => {
  return rawNodes.value.map(n => ({
    value: n.id,
    label: `${n.name} (${getTypeLabel(n.type)})`
  }))
})

// 加载图谱数据
const loadGraphData = async () => {
  loading.value = true
  try {
    const [graphRes, statsRes] = await Promise.all([
      getKnowledgeGraphData(200),
      getKnowledgeGraphStats()
    ])
    const data = graphRes.data || graphRes
    rawNodes.value = data.nodes || []
    rawEdges.value = data.edges || []
    displayedNodeCount.value = rawNodes.value.length
    stats.value = statsRes.data || statsRes || {}
    renderGraph()
  } catch (e) {
    console.error('加载图谱数据失败', e)
    ElMessage.error('加载图谱数据失败')
  } finally {
    loading.value = false
  }
}

// 渲染 ECharts 星球图谱
const renderGraph = () => {
  if (!chartInstance) return

  const degrees = computeNodeDegrees()
  const maxDegree = Math.max(1, ...degrees.values())
  const nodeTypeMap = new Map(rawNodes.value.map((n) => [String(n.id), n.type || 'unknown']))

  const nodes = rawNodes.value.map((n) => {
    const degree = degrees.get(n.id) || 0
    const importance = degree / maxDegree
    const size = 28 + importance * 44
    const hasRing = importance > 0.6
    const color = getTypeColor(n.type)
    const glow = planetPalettes[n.type]?.glow || planetPalettes.unknown.glow
    const seed = [...String(n.id)].reduce((sum, char) => sum + char.charCodeAt(0), 0)

    if (!planetRotations.value.has(n.id)) {
      planetRotations.value.set(n.id, Math.random() * 360)
    }

    return {
      id: String(n.id),
      name: n.name,
      value: degree,
      symbol: createPlanetSvg(color, hasRing, seed),
      symbolSize: size,
      symbolRotate: planetRotations.value.get(n.id),
      itemStyle: {
        shadowBlur: 26 + importance * 20,
        shadowColor: glow,
        borderColor: glow,
        borderWidth: 0
      },
      category: n.type || 'unknown',
      original: n,
      importance,
      label: {
        show: true,
        formatter: '{b}',
        fontSize: 11 + importance * 3,
        color: '#E2E8F0',
        textBorderColor: '#050816',
        textBorderWidth: 2.5,
        position: 'bottom',
        distance: 8
      },
      emphasis: {
        label: {
          show: true,
          color: '#FFFFFF',
          fontSize: 12 + importance * 3,
          textBorderColor: '#050816',
          textBorderWidth: 3
        },
        itemStyle: {
          shadowBlur: 46 + importance * 22,
          shadowColor: glow
        }
      }
    }
  })

  const edges = rawEdges.value.map((e, index) => {
    const sourceColor = getTypeColor(nodeTypeMap.get(String(e.sourceId)))
    const targetColor = getTypeColor(nodeTypeMap.get(String(e.targetId)))
    const effectColor = index % 3 === 0 ? targetColor : '#FFF7E0'

    return {
      source: String(e.sourceId),
      target: String(e.targetId),
      value: e.relation || '关联',
      label: {
        show: false,
        formatter: e.relation || '关联',
        fontSize: 10,
        color: '#FDE68A',
        textBorderColor: '#050816',
        textBorderWidth: 2
      },
      emphasis: {
        label: {
          show: true,
          formatter: e.relation || '关联',
          fontSize: 10,
          color: '#FDE68A',
          backgroundColor: 'rgba(5, 8, 22, 0.86)',
          borderColor: 'rgba(253, 230, 138, 0.28)',
          borderWidth: 1,
          padding: [3, 7],
          borderRadius: 6
        },
        lineStyle: {
          width: 2.2,
          opacity: 0.96,
          shadowBlur: 20,
          shadowColor: effectColor
        }
      },
      lineStyle: {
        curveness: 0.26 + (index % 4) * 0.035,
        width: 1.4,
        type: 'dashed',
        opacity: 0.52,
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 1,
          colorStops: [
            { offset: 0, color: sourceColor },
            { offset: 1, color: targetColor }
          ]
        },
        shadowBlur: 16,
        shadowColor: sourceColor
      },
      effect: {
        show: true,
        period: 4.2 + (index % 5) * 0.7,
        trailLength: 0.8,
        color: effectColor,
        symbol: 'circle',
        symbolSize: 4.5,
        opacity: 0.95
      },
      original: e
    }
  })

  const option = {
    // 让动态星场和星云从画布下方透出来
    backgroundColor: 'transparent',
    graphic: {
      elements: Array.from({ length: 90 }, () => ({
        type: 'circle',
        shape: {
          cx: Math.random() * 1000,
          cy: Math.random() * 800,
          r: Math.random() * 1.4 + 0.4
        },
        style: {
          fill: Math.random() > 0.82 ? '#FCD34D' : '#E2E8F0',
          opacity: Math.random() * 0.5 + 0.12
        },
        silent: true
      }))
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(9, 13, 30, 0.9)',
      borderColor: 'rgba(129, 140, 248, 0.45)',
      borderWidth: 1,
      padding: [10, 13],
      textStyle: { color: '#E2E8F0', fontSize: 12 },
      extraCssText: 'border-radius: 12px; backdrop-filter: blur(14px); box-shadow: 0 18px 40px rgba(0,0,0,.4);',
      formatter: (params) => {
        if (params.dataType === 'node') {
          const type = params.data?.original?.type || 'unknown'
          return `<div style="font-weight:700;color:#F8FAFC;margin-bottom:5px">${escapeHtml(params.name)}</div>
            <div style="display:flex;align-items:center;gap:6px;color:#94A3B8;font-size:11px">
              <span style="width:7px;height:7px;border-radius:50%;background:${getTypeColor(type)};box-shadow:0 0 8px ${getTypeColor(type)}"></span>
              ${escapeHtml(getTypeLabel(type))} · 引力连接 ${params.value}
            </div>`
        }
        return escapeHtml(params.data?.value || '关联')
      }
    },
    animationDuration: 950,
    animationEasingUpdate: 'cubicOut',
    series: [
      {
        type: 'graph',
        layout: currentLayout.value,
        data: nodes,
        links: edges,
        roam: true,
        draggable: true,
        focusNodeAdjacency: true,
        edgeSymbol: ['none', 'none'],
        labelLayout: { hideOverlap: true },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 3, opacity: 0.9, shadowBlur: 18 },
          itemStyle: { shadowBlur: 40, shadowColor: '#fff' }
        },
        blur: {
          itemStyle: { opacity: 0.32 },
          label: { opacity: 0.34 },
          lineStyle: { opacity: 0.1 }
        },
        force: {
          repulsion: 480,
          gravity: 0.07,
          edgeLength: [100, 230],
          layoutAnimation: true
        },
        circular: {
          rotateLabel: false
        },
        lineStyle: {
          curveness: 0.3
        }
      }
    ]
  }

  chartInstance.setOption(option, true)
  startPlanetRotation()
}

// 星球自转动画
const startPlanetRotation = () => {
  if (rotationTimer) return
  rotationTimer = setInterval(() => {
    if (!chartInstance) return
    const option = chartInstance.getOption()
    const series = option.series?.[0]
    if (!series || !series.data) return
    let changed = false
    series.data.forEach(node => {
      if (node.symbol && node.symbol.startsWith('image://')) {
        const current = planetRotations.value.get(node.original?.id) || 0
        const next = (current + 0.9) % 360
        planetRotations.value.set(node.original.id, next)
        node.symbolRotate = next
        changed = true
      }
    })
    if (changed) {
      chartInstance.setOption({ series: [{ data: series.data }] }, false, true)
    }
  }, 120)
}

// 搜索节点
const handleSearch = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    clearSearch()
    return
  }
  try {
    const res = await searchKnowledgeNodes(keyword)
    const foundNodes = res.data || res || []
    if (foundNodes.length === 0) {
      ElMessage.info('未找到匹配节点')
      return
    }

    const foundIds = foundNodes.map(n => String(n.id))
    chartInstance.dispatchAction({ type: 'downplay' })
    foundIds.forEach(id => {
      chartInstance.dispatchAction({ type: 'highlight', seriesIndex: 0, name: id })
    })

    const firstId = foundIds[0]
    const option = chartInstance.getOption()
    const node = option.series[0].data.find(n => n.id === firstId)
    if (node && node.x != null && node.y != null) {
      chartInstance.dispatchAction({
        type: 'focusNodeAdjacency',
        seriesIndex: 0,
        dataIndex: option.series[0].data.indexOf(node)
      })
    }

    ElMessage.success(`找到 ${foundNodes.length} 个匹配节点`)
  } catch (e) {
    console.error('搜索失败', e)
    ElMessage.error('搜索失败')
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  if (chartInstance) {
    chartInstance.dispatchAction({ type: 'downplay' })
  }
}

// 选择节点
const selectNodeById = async (id) => {
  if (!id) return
  try {
    const res = await getKnowledgeNodeDetail(id)
    const data = res.data || res
    selectedNode.value = data.node
    outgoingEdges.value = data.outgoingEdges || []
    incomingEdges.value = data.incomingEdges || []

    highlightNode(id)
  } catch (e) {
    console.error('获取节点详情失败', e)
    ElMessage.error('获取节点详情失败')
  }
}

const highlightNode = (id) => {
  if (!chartInstance) return
  chartInstance.dispatchAction({ type: 'downplay' })
  const data = chartInstance.getOption()?.series?.[0]?.data || []
  const index = data.findIndex((node) => String(node.id) === String(id))
  if (index >= 0) {
    chartInstance.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: index })
  }
}

const clearSelection = () => {
  selectedNode.value = null
  outgoingEdges.value = []
  incomingEdges.value = []
  chartInstance?.dispatchAction({ type: 'downplay' })
}

// 视图控制
const fitGraph = () => {
  if (chartInstance) {
    chartInstance.dispatchAction({ type: 'restore' })
  }
}

const resetView = () => {
  if (chartInstance) {
    chartInstance.dispatchAction({ type: 'restore' })
    currentLayout.value = 'force'
    renderGraph()
  }
}

const toggleLayout = () => {
  currentLayout.value = currentLayout.value === 'force' ? 'circular' : 'force'
  renderGraph()
}

const toggleTypeFilter = (type) => {
  selectedType.value = selectedType.value === type ? null : type
  if (!chartInstance) return

  const option = chartInstance.getOption()
  const series = option.series[0]
  series.data.forEach(node => {
    node.itemStyle = node.itemStyle || {}
    if (selectedType.value && node.original.type !== selectedType.value) {
      node.itemStyle.opacity = 0.12
      node.label = { show: false }
    } else {
      node.itemStyle.opacity = 1
      node.label = { show: true, formatter: '{b}', fontSize: 11 + (node.importance || 0) * 3, color: '#E2E8F0', textBorderColor: '#0B1026', textBorderWidth: 2 }
    }
  })
  chartInstance.setOption(option)
}

// 添加节点
const openAddNode = () => {
  nodeForm.value = { name: '', type: 'concept' }
  showAddNodeDialog.value = true
}

const addNode = async () => {
  if (!nodeForm.value.name.trim()) {
    ElMessage.warning('请输入节点名称')
    return
  }
  saving.value = true
  try {
    await addKnowledgeNode({
      name: nodeForm.value.name.trim(),
      type: nodeForm.value.type
    })
    ElMessage.success('节点添加成功')
    showAddNodeDialog.value = false
    loadGraphData()
  } catch (e) {
    ElMessage.error('添加节点失败')
  } finally {
    saving.value = false
  }
}

// 添加关系
const openAddEdge = () => {
  edgeForm.value = { sourceId: null, targetId: null, relation: '关联' }
  showAddEdgeDialog.value = true
}

const openAddEdgeWithSource = (node) => {
  edgeForm.value = { sourceId: node.id, targetId: null, relation: '关联' }
  showAddEdgeDialog.value = true
}

const addEdge = async () => {
  if (!edgeForm.value.sourceId || !edgeForm.value.targetId) {
    ElMessage.warning('请选择源节点和目标节点')
    return
  }
  if (edgeForm.value.sourceId === edgeForm.value.targetId) {
    ElMessage.warning('源节点和目标节点不能相同')
    return
  }
  saving.value = true
  try {
    await addKnowledgeEdge({
      sourceId: edgeForm.value.sourceId,
      targetId: edgeForm.value.targetId,
      relation: edgeForm.value.relation.trim() || '关联'
    })
    ElMessage.success('关系添加成功')
    showAddEdgeDialog.value = false
    loadGraphData()
  } catch (e) {
    const msg = e.response?.data?.message || '添加关系失败'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

// 删除节点
const deleteSelectedNode = async () => {
  if (!selectedNode.value) return
  try {
    await ElMessageBox.confirm('确定要删除该节点及其关联关系吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteKnowledgeNode(selectedNode.value.id)
    ElMessage.success('节点已删除')
    selectedNode.value = null
    outgoingEdges.value = []
    incomingEdges.value = []
    loadGraphData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const goToKnowledge = () => {
  router.push('/knowledge')
}

const nextTourStep = () => {
  if (tourStep.value < 3) {
    tourStep.value++
  } else {
    showTour.value = false
    tourStep.value = 0
    localStorage.setItem('kg-tour-seen', 'true')
  }
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)

  chartInstance.on('click', (params) => {
    if (params.dataType === 'node') {
      selectNodeById(params.data.original.id)
    }
  })

  chartInstance.on('dblclick', (params) => {
    if (params.dataType === 'node') {
      chartInstance.dispatchAction({
        type: 'focusNodeAdjacency',
        seriesIndex: 0,
        dataIndex: params.dataIndex
      })
    }
  })

  const resizeHandler = () => chartInstance && chartInstance.resize()
  window.addEventListener('resize', resizeHandler)

  return () => {
    window.removeEventListener('resize', resizeHandler)
    chartInstance.dispose()
    chartInstance = null
  }
}

onMounted(() => {
  initChart()
  loadGraphData()

  const hasSeenTour = localStorage.getItem('kg-tour-seen')
  if (!hasSeenTour && rawNodes.value.length > 0) {
    setTimeout(() => {
      showTour.value = true
      localStorage.setItem('kg-tour-seen', 'true')
    }, 800)
  }
})

onUnmounted(() => {
  if (rotationTimer) {
    clearInterval(rotationTimer)
    rotationTimer = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

watch(rawNodes, () => {
  nextTick(() => renderGraph())
})
</script>

<style lang="scss" scoped>
.kg-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(56% 44% at 88% 0%, var(--content-glow-b), transparent 60%),
    radial-gradient(48% 40% at 6% 8%, var(--content-glow-a), transparent 60%),
    var(--color-bg);
}

.main-content {
  flex: 1;
  padding: calc(var(--header-height) + 30px) 24px 48px;
}

.page-container {
  max-width: 1560px;
  margin: 0 auto;
  width: 100%;
}

/* ===== 页头 ===== */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.title-area {
  position: relative;
  min-width: 0;
}

.title-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--content-hero-line);
  background: var(--content-eyebrow);
  color: var(--content-ink-3);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4285f4, #a855f7);
    box-shadow: 0 0 10px rgba(66, 133, 244, 0.6);
  }
}

.page-header h1 {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: clamp(28px, 3.4vw, 38px);
  font-weight: 800;
  letter-spacing: -0.02em;
}

.gradient-title {
  background: linear-gradient(135deg, var(--content-ink) 8%, var(--color-primary) 58%, #a855f7 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.title-glyph {
  color: var(--color-primary);
  font-size: 30px;
  filter: drop-shadow(0 0 12px var(--content-orb-a));
  animation: slowRotate 18s linear infinite;
}

.title-sub {
  margin: 8px 0 0;
  color: var(--content-ink-2);
  font-size: 14px;
  letter-spacing: 0.01em;
}

@keyframes slowRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.header-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
}

:deep(.galaxy-btn.el-button) {
  height: 40px;
  padding: 0 20px;
  border: none;
  border-radius: 12px;
  color: #fff;
  font-weight: 600;
  background: linear-gradient(120deg, #1a73e8, #6f5cf6 62%, #8b5cf6);
  box-shadow: 0 10px 24px rgba(47, 99, 235, 0.28);

  &:hover {
    color: #fff;
    filter: brightness(1.06);
    box-shadow: 0 14px 30px rgba(47, 99, 235, 0.34);
  }
}

:deep(.galaxy-btn-plain.el-button) {
  height: 40px;
  padding: 0 18px;
  border-radius: 12px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-2);

  &:hover {
    border-color: var(--content-line-strong);
    color: var(--color-primary);
    background: var(--content-surface);
  }
}

/* ===== 工作区 ===== */
.workspace {
  display: grid;
  grid-template-columns: 268px minmax(0, 1fr) 304px;
  gap: 18px;
  align-items: start;
}

.panel-card {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  margin-bottom: 16px;

  h3 {
    margin: 0 0 14px;
    color: var(--content-ink);
    font-size: 14px;
    font-weight: 700;
  }
}

.nebula-card {
  background:
    radial-gradient(120% 100% at 100% 0%, var(--content-glow-b), transparent 58%),
    var(--content-surface);
  border: 1px solid var(--content-line);
  box-shadow: var(--content-shadow);

  h3 {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12.5px;
    letter-spacing: 0.1em;
    text-transform: uppercase;

    .el-icon {
      color: var(--color-primary);
      font-size: 16px;
    }
  }
}

.stat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--content-line);

  &:last-child {
    border-bottom: none;
    padding-bottom: 2px;
  }

  .stat-label {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--content-ink-2);
    font-size: 12.5px;

    .el-icon {
      color: var(--content-ink-3);
    }
  }

  .stat-value {
    color: var(--content-ink);
    font-size: 19px;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
  }

  .glow-number {
    color: var(--color-primary);

    &.cyan {
      color: #0891b2;
    }

    &.amber {
      color: #d97706;
    }
  }
}

:global([data-theme='dark']) .stat-item .glow-number {
  color: #a5b4fc;

  &.cyan {
    color: #67e8f9;
  }

  &.amber {
    color: #fcd34d;
  }
}

.type-legend {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 12px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--content-surface-tint);
  }

  &.active {
    border-color: var(--content-line-strong);
    background: var(--content-surface-tint);
  }

  .type-dot {
    flex-shrink: 0;
    width: 11px;
    height: 11px;
    border-radius: 50%;
  }

  .type-name {
    flex: 1;
    color: var(--content-ink);
    font-size: 13.5px;
  }

  .type-count {
    padding: 2px 8px;
    border-radius: 999px;
    background: var(--content-chip);
    border: 1px solid var(--content-chip-line);
    color: var(--content-ink-3);
    font-size: 11.5px;
    font-variant-numeric: tabular-nums;
  }
}

.clear-type {
  margin-top: 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-primary);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.hint-card {
  display: flex;
  gap: 10px;
  margin-bottom: 0;
  color: var(--content-ink-2);
  font-size: 12.5px;
  line-height: 1.6;

  .hint-icon {
    flex-shrink: 0;
    color: var(--color-primary);
    font-size: 17px;
  }

  p {
    margin: 0;
  }
}

/* ===== 星图画布 ===== */
.graph-area {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 640px;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(129, 140, 248, 0.3);
  background:
    radial-gradient(70% 60% at 50% 42%, rgba(35, 31, 92, 0.56), transparent 72%),
    radial-gradient(40% 36% at 16% 14%, rgba(14, 165, 233, 0.12), transparent 70%),
    radial-gradient(36% 30% at 84% 78%, rgba(236, 72, 153, 0.1), transparent 70%),
    #030712;
  box-shadow:
    0 30px 80px rgba(15, 23, 42, 0.34),
    0 0 70px rgba(79, 70, 229, 0.12),
    inset 0 0 90px rgba(99, 102, 241, 0.09);

  .star-field-bg {
    position: absolute;
    inset: 0;
    z-index: 0;
  }
}

.galaxy-core {
  position: absolute;
  top: 45%;
  left: 50%;
  z-index: 0;
  width: 560px;
  height: 420px;
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(99, 102, 241, 0.22) 0%, rgba(30, 27, 75, 0.12) 46%, transparent 72%);
  filter: blur(28px);
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.galaxy-aurora {
  position: absolute;
  top: -30%;
  left: -10%;
  z-index: 0;
  width: 120%;
  height: 160%;
  border-radius: 50%;
  background: conic-gradient(
    from 80deg,
    transparent 0deg,
    rgba(99, 102, 241, 0.34) 58deg,
    transparent 125deg,
    rgba(14, 165, 233, 0.24) 190deg,
    transparent 258deg,
    rgba(236, 72, 153, 0.17) 320deg,
    transparent 360deg
  );
  filter: blur(72px);
  opacity: 0.18;
  pointer-events: none;
  will-change: transform;
  animation: auroraSpin 92s linear infinite;

  &.aurora-b {
    top: -12%;
    left: 5%;
    width: 92%;
    height: 128%;
    opacity: 0.12;
    animation-duration: 136s;
    animation-direction: reverse;
  }
}

@keyframes auroraSpin {
  to { transform: rotate(360deg); }
}

.graph-rings {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;

  span {
    position: absolute;
    border-radius: 50%;
    border: 1px dashed rgba(129, 140, 248, 0.11);
    will-change: transform;
    animation: ringSpin 120s linear infinite;
  }

  .ring-1 {
    width: 340px;
    height: 340px;
  }

  .ring-2 {
    width: 540px;
    height: 540px;
    border-color: rgba(56, 189, 248, 0.08);
    animation-duration: 160s;
    animation-direction: reverse;
  }

  .ring-3 {
    width: 760px;
    height: 760px;
    border-color: rgba(236, 72, 153, 0.06);
    animation-duration: 210s;
  }
}

@keyframes ringSpin {
  to { transform: rotate(360deg); }
}

.nebula-belt {
  position: absolute;
  z-index: 0;
  border-radius: 999px;
  filter: blur(60px);
  opacity: 0.3;
  pointer-events: none;

  &.belt-1 {
    width: 80%;
    height: 120px;
    top: 15%;
    left: 10%;
    background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.35), rgba(59, 130, 246, 0.28), transparent);
    animation: beltDrift 28s ease-in-out infinite alternate;
  }

  &.belt-2 {
    width: 70%;
    height: 90px;
    bottom: 20%;
    right: 5%;
    background: linear-gradient(90deg, transparent, rgba(236, 72, 153, 0.28), rgba(99, 102, 241, 0.22), transparent);
    animation: beltDrift 22s ease-in-out infinite alternate-reverse;
  }
}

.nebula-cloud {
  position: absolute;
  z-index: 0;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.26;
  pointer-events: none;

  &.cloud-1 {
    width: 260px;
    height: 260px;
    top: 8%;
    right: 12%;
    background: radial-gradient(circle, rgba(124, 107, 255, 0.45) 0%, transparent 70%);
    animation: cloudFloat 18s ease-in-out infinite alternate;
  }

  &.cloud-2 {
    width: 320px;
    height: 320px;
    bottom: 10%;
    left: 8%;
    background: radial-gradient(circle, rgba(14, 165, 233, 0.35) 0%, transparent 70%);
    animation: cloudFloat 24s ease-in-out infinite alternate-reverse;
  }
}

@keyframes beltDrift {
  from { transform: translateX(-4%) scaleY(1); opacity: 0.24; }
  to { transform: translateX(4%) scaleY(1.15); opacity: 0.38; }
}

@keyframes cloudFloat {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(20px, -24px) scale(1.08); }
}

/* 画布内控件固定暗色星舰风格 */
.toolbar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(129, 140, 248, 0.18);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.72), rgba(8, 13, 28, 0.52));
  backdrop-filter: blur(12px);
  box-shadow: 0 14px 32px rgba(2, 6, 23, 0.22);

  .search-wrap {
    display: flex;
    flex: 1;
    min-width: 260px;
    gap: 8px;

    .el-input {
      max-width: 340px;
    }
  }

  .tool-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
  }

  :deep(.el-input__wrapper) {
    background: rgba(15, 23, 42, 0.72) !important;
    box-shadow: 0 0 0 1px rgba(129, 140, 248, 0.25) inset !important;
  }

  :deep(.el-input__inner) {
    color: #e2e8f0 !important;

    &::placeholder {
      color: #94a3b8 !important;
    }
  }

  :deep(.el-input__prefix-inner) {
    color: #94a3b8 !important;
  }

  :deep(.el-button) {
    background: rgba(30, 41, 59, 0.55);
    border-color: rgba(129, 140, 248, 0.22);
    color: #c7d2fe;

    &:hover {
      background: rgba(99, 102, 241, 0.22);
      border-color: rgba(129, 140, 248, 0.45);
      color: #fff;
    }
  }

  :deep(.el-button--primary) {
    border: none;
    color: #fff;
    background: linear-gradient(120deg, #4f46e5, #7c3aed);
    box-shadow: 0 8px 20px rgba(79, 70, 229, 0.32);
  }

  :deep(.el-divider--vertical) {
    border-left-color: rgba(129, 140, 248, 0.22);
  }
}

.chart-wrapper {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 560px;
}

.chart-container {
  width: 100%;
  height: 100%;
  min-height: 560px;
  background: transparent;
}

.graph-vignette {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: radial-gradient(ellipse at 50% 48%, transparent 44%, rgba(2, 6, 23, 0.26) 78%, rgba(2, 6, 23, 0.55) 100%);
  box-shadow: inset 0 0 90px rgba(2, 6, 23, 0.4);
}

.galaxy-loading {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 15px;
  background: rgba(2, 6, 23, 0.58);
  backdrop-filter: blur(6px);
  color: #a5b4fc;
  font-size: 12px;
  letter-spacing: 0.14em;

  p {
    margin: 0;
  }
}

.gl-orbit {
  position: relative;
  width: 64px;
  height: 64px;

  i {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    border: 1px solid rgba(129, 140, 248, 0.24);
    border-top-color: #a5b4fc;
    animation: glSpin 1.4s linear infinite;

    &:nth-child(2) {
      inset: 9px;
      border-top-color: #67e8f9;
      animation-duration: 1.9s;
      animation-direction: reverse;
    }

    &:nth-child(3) {
      inset: 18px;
      border-top-color: #f0abfc;
      animation-duration: 2.4s;
    }
  }

  b {
    position: absolute;
    inset: 27px;
    border-radius: 50%;
    background: radial-gradient(circle at 35% 30%, #fff, #8b5cf6 70%);
    box-shadow: 0 0 22px rgba(139, 92, 246, 0.85);
  }
}

@keyframes glSpin {
  to { transform: rotate(360deg); }
}

.graph-hud {
  position: absolute;
  left: 18px;
  bottom: 14px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  max-width: calc(100% - 36px);
  padding: 7px 13px;
  border-radius: 999px;
  border: 1px solid rgba(129, 140, 248, 0.2);
  background: rgba(8, 12, 24, 0.62);
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 26px rgba(2, 6, 23, 0.32);
  color: #94a3b8;
  font-size: 11.5px;
  white-space: nowrap;
  pointer-events: none;

  b {
    color: #e2e8f0;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }

  .hud-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #34d399;
    animation: hudPulse 2.4s ease-in-out infinite;
  }

  .hud-sep {
    width: 1px;
    height: 10px;
    background: rgba(148, 163, 184, 0.28);
  }
}

@keyframes hudPulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(52, 211, 153, 0.14); }
  50% { box-shadow: 0 0 0 6px rgba(52, 211, 153, 0.04), 0 0 14px rgba(52, 211, 153, 0.6); }
}

.empty-state {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(2, 6, 23, 0.72);

  .empty-icon {
    margin-bottom: 16px;
    color: #a5b4fc;
    font-size: 64px;

    .el-icon {
      filter: drop-shadow(0 0 16px rgba(129, 140, 248, 0.5));
    }
  }

  .empty-hint {
    max-width: 320px;
    margin: 12px 0 20px;
    color: #94a3b8;
    font-size: 14px;
  }

  .empty-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
  }

  :deep(.el-empty__description p) {
    color: #cbd5e1;
  }

  :deep(.el-button) {
    background: rgba(30, 41, 59, 0.6);
    border-color: rgba(129, 140, 248, 0.25);
    color: #c7d2fe;
  }

  :deep(.el-button--primary) {
    border: none;
    color: #fff;
    background: linear-gradient(120deg, #4f46e5, #7c3aed);
  }
}

/* ===== 详情面板 ===== */
.detail-panel {
  position: sticky;
  top: calc(var(--header-height) + 20px);
  display: flex;
  flex-direction: column;
  min-height: 640px;
  border-radius: 24px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  overflow: hidden;

  &.nebula-panel {
    background:
      radial-gradient(120% 80% at 100% 0%, var(--content-glow-b), transparent 55%),
      var(--content-surface);
  }

  .panel-header {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 16px 18px;
    border-bottom: 1px solid var(--content-line);

    &::after {
      content: '';
      position: absolute;
      right: 18px;
      bottom: -1px;
      left: 18px;
      height: 1px;
      background: linear-gradient(90deg, var(--node-accent, var(--color-primary)), transparent);
      opacity: 0.6;
    }

    .node-title {
      display: flex;
      align-items: center;
      gap: 11px;
      min-width: 0;

      h3 {
        overflow: hidden;
        margin: 0;
        color: var(--content-ink);
        font-size: 17px;
        font-weight: 600;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .node-orb {
        flex-shrink: 0;
        width: 21px;
        height: 21px;
        border-radius: 50%;
      }
    }

    :deep(.el-button) {
      color: var(--content-ink-3);

      &:hover {
        color: var(--content-ink);
        background: var(--content-surface-tint);
      }
    }
  }

  .panel-body {
    flex: 1;
    overflow-y: auto;
    padding: 16px 18px 20px;
  }

  .info-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid var(--content-line);
    font-size: 13.5px;

    .label {
      color: var(--content-ink-3);
    }

    .value {
      color: var(--content-ink);
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 12.5px;
    }
  }

  .relations-section {
    margin-top: 20px;

    .section-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;
      color: var(--content-ink);
      font-size: 13.5px;
      font-weight: 600;
    }

    .empty-text {
      padding: 8px 0;
      color: var(--content-ink-3);
      font-size: 12.5px;
    }
  }

  .relation-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .relation-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 11px;
    border-radius: 12px;
    border: 1px solid var(--content-line);
    background: var(--content-surface-tint);
    font-size: 12.5px;
    cursor: pointer;
    transition: all var(--transition-fast);

    &:hover {
      border-color: var(--content-line-strong);
      background: var(--content-chip);
    }

    .node-name {
      flex: 1;
      min-width: 0;
      overflow: hidden;
      color: var(--content-ink);
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .relation-name {
      flex-shrink: 0;
      color: var(--color-primary);
      font-weight: 500;
    }

    .el-icon {
      flex-shrink: 0;
      color: var(--content-ink-3);
    }
  }

  .panel-actions {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 24px;

    :deep(.el-button) {
      margin-left: 0;
    }
  }
}

.no-selection {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  text-align: center;

  .empty-orbit {
    position: relative;
    width: 132px;
    height: 132px;
    margin-bottom: 20px;
  }

  .eo-ring {
    position: absolute;
    border-radius: 50%;
    border: 1px solid var(--content-line-strong);

    &.eo-ring-1 {
      inset: 0;
      border-style: dashed;
      animation: orbitSpin 24s linear infinite;
    }

    &.eo-ring-2 {
      inset: 18px;
      opacity: 0.65;
      animation: orbitSpin 18s linear infinite reverse;
    }
  }

  .eo-core {
    position: absolute;
    top: 50%;
    left: 50%;
    display: grid;
    place-items: center;
    width: 48px;
    height: 48px;
    margin: -24px 0 0 -24px;
    border-radius: 16px;
    color: #fff;
    font-size: 20px;
    background: linear-gradient(135deg, #1a73e8, #8b5cf6);
    box-shadow: 0 14px 30px rgba(70, 90, 220, 0.28);
  }

  h4 {
    margin: 0 0 8px;
    color: var(--content-ink);
    font-size: 16px;
    font-weight: 600;
  }

  p {
    max-width: 220px;
    margin: 0;
    color: var(--content-ink-2);
    font-size: 13px;
    line-height: 1.7;
  }

  .no-selection-tips {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 6px;
    margin-top: 18px;

    span {
      padding: 5px 10px;
      border-radius: 999px;
      border: 1px solid var(--content-chip-line);
      background: var(--content-chip);
      color: var(--content-ink-3);
      font-size: 11.5px;
    }
  }
}

@keyframes orbitSpin {
  to { transform: rotate(360deg); }
}

/* ===== 帮助与引导 ===== */
.help-content {
  section {
    margin-bottom: 24px;

    h4 {
      margin: 0 0 10px;
      color: var(--content-ink);
      font-size: 15px;
      font-weight: 600;
    }

    p,
    li {
      color: var(--content-ink-2);
      font-size: 13.5px;
      line-height: 1.75;
    }

    ol,
    ul {
      margin: 0;
      padding-left: 18px;
    }
  }
}

.tour-content {
  min-height: 80px;
  margin-top: 20px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);

  p {
    margin: 0;
    color: var(--content-ink-2);
    font-size: 14px;
    line-height: 1.7;
  }
}

@media (max-width: 1320px) {
  .workspace {
    grid-template-columns: 240px minmax(0, 1fr) 280px;
  }
}

@media (max-width: 1200px) {
  .workspace {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .detail-panel {
    position: static;
    grid-column: 1 / -1;
    min-height: auto;
  }
}

@media (max-width: 900px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .graph-area {
    order: 1;
    min-height: 520px;
  }

  .stats-panel {
    order: 2;
  }

  .detail-panel {
    order: 3;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .header-actions {
    width: 100%;
  }

  .panel-card {
    margin-bottom: 12px;
  }

  .stats-panel {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;

    .panel-card {
      margin-bottom: 0;
    }

    .hint-card {
      grid-column: 1 / -1;
    }
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: calc(var(--header-height) + 22px) 16px 40px;
  }

  .graph-area {
    min-height: 460px;
    border-radius: 20px;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
    padding: 12px;

    .search-wrap {
      width: 100%;
      min-width: 0;

      .el-input {
        max-width: none;
      }
    }

    .tool-actions {
      justify-content: flex-end;
    }
  }

  .chart-wrapper,
  .chart-container {
    min-height: 400px;
  }

  .graph-hud {
    left: 12px;
    bottom: 10px;
    padding: 6px 10px;
    font-size: 10.5px;

    .hud-sep:last-of-type {
      display: none;
    }
  }
}

@media (max-width: 560px) {
  .stats-panel {
    grid-template-columns: 1fr;
  }

  .page-header h1 {
    font-size: 28px;
  }

  .title-sub {
    font-size: 13px;
  }

  .graph-hud {
    gap: 5px;
  }
}
</style>
