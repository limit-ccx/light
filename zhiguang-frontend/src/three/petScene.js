import * as THREE from 'three'

/* 世界尺寸：立绘统一高度，宽度按各自宽高比 */
const FIG_H = 1.6
const GROUND_Y = -0.8
const SEG_X = 40
const SEG_Y = 56
const DEPTH_W = 96

const lerp = THREE.MathUtils.lerp
const ci = (v, hi) => (v < 0 ? 0 : v > hi ? hi : v)

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error(`桌宠贴图加载失败: ${src}`))
    img.src = src
  })
}

/* 可分离盒式模糊，用于把 alpha 图变成“距轮廓边缘的距离”场 */
function boxBlur(src, w, h, r) {
  const tmp = new Float32Array(w * h)
  const out = new Float32Array(w * h)
  const div = r * 2 + 1
  for (let y = 0; y < h; y++) {
    const row = y * w
    let acc = 0
    for (let i = -r; i <= r; i++) acc += src[row + ci(i, w - 1)]
    for (let x = 0; x < w; x++) {
      tmp[row + x] = acc / div
      acc -= src[row + ci(x - r, w - 1)]
      acc += src[row + ci(x + r + 1, w - 1)]
    }
  }
  for (let x = 0; x < w; x++) {
    let acc = 0
    for (let i = -r; i <= r; i++) acc += tmp[ci(i, h - 1) * w + x]
    for (let y = 0; y < h; y++) {
      out[y * w + x] = acc / div
      acc -= tmp[ci(y - r, h - 1) * w + x]
      acc += tmp[ci(y + r + 1, h - 1) * w + x]
    }
  }
  return out
}

/* 立绘 alpha -> 归一化景深场（轮廓为 0，身体中心最厚） */
function depthField(img) {
  const w = DEPTH_W
  const h = Math.max(2, Math.round((img.naturalHeight / img.naturalWidth) * DEPTH_W))
  const c = document.createElement('canvas')
  c.width = w
  c.height = h
  const ctx = c.getContext('2d', { willReadFrequently: true })
  ctx.drawImage(img, 0, 0, w, h)
  const { data } = ctx.getImageData(0, 0, w, h)

  const a = new Float32Array(w * h)
  for (let i = 0; i < a.length; i++) a[i] = data[i * 4 + 3] / 255

  const r = Math.max(2, Math.round(w / 11))
  let d = boxBlur(a, w, h, r)
  d = boxBlur(d, w, h, r)

  let max = 0
  for (let i = 0; i < d.length; i++) if (d[i] > max) max = d[i]
  if (max > 0) for (let i = 0; i < d.length; i++) d[i] /= max
  return { depth: d, w, h }
}

function radialTexture(stops, size = 256) {
  const c = document.createElement('canvas')
  c.width = c.height = size
  const ctx = c.getContext('2d')
  const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
  stops.forEach(([o, col]) => g.addColorStop(o, col))
  ctx.fillStyle = g
  ctx.fillRect(0, 0, size, size)
  const t = new THREE.CanvasTexture(c)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

/* 单个动作 -> 带真实起伏的网格（顶点法线由几何体本身算出） */
function buildFigure(img, tex, depth) {
  const aspect = img.naturalWidth / img.naturalHeight
  const w = FIG_H * aspect
  const geo = new THREE.PlaneGeometry(w, FIG_H, SEG_X, SEG_Y)
  const pos = geo.attributes.position
  const uv = geo.attributes.uv
  const bulge = w * 0.19
  const dw = depth.w
  const dh = depth.h

  for (let i = 0; i < pos.count; i++) {
    const px = ci(Math.round(uv.getX(i) * (dw - 1)), dw - 1)
    // UV 的 v 向上，画布行号向下
    const py = ci(Math.round((1 - uv.getY(i)) * (dh - 1)), dh - 1)
    pos.setZ(i, depth.depth[py * dw + px] * bulge)
  }
  geo.computeVertexNormals()
  pos.needsUpdate = true

  const mat = new THREE.MeshStandardMaterial({
    map: tex,
    emissive: new THREE.Color(0xffffff),
    emissiveMap: tex,
    emissiveIntensity: 0.6,
    transparent: true,
    alphaTest: 0.06,
    roughness: 0.58,
    metalness: 0.04,
    side: THREE.DoubleSide
  })
  const mesh = new THREE.Mesh(geo, mat)
  mesh.position.y = GROUND_Y + FIG_H / 2
  return { mesh, w }
}

export function createPetScene({ host, poses, startKey, dark = false, reducedMotion = false }) {
  if (!host || !poses?.length) throw new Error('createPetScene: 缺少 host 或 poses')

  const W = () => host.clientWidth || 128
  const H = () => host.clientHeight || 194

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'default' })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.75))
  renderer.setSize(W(), H(), false)
  renderer.setClearAlpha(0)
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.domElement.style.cssText = 'width:100%;height:100%;display:block'
  host.appendChild(renderer.domElement)

  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(32, W() / H(), 0.1, 20)
  camera.position.set(0, 0.3, 3.6)
  camera.lookAt(0, -0.02, 0)

  const root = new THREE.Group()
  scene.add(root)

  /* ---------- 角色所在的立体层 ---------- */
  const figure = new THREE.Group()
  root.add(figure)

  /* ---------- 地面：接触阴影 + 光晕环 ---------- */
  const ground = new THREE.Group()
  ground.position.y = GROUND_Y
  root.add(ground)

  const shadowTex = radialTexture([
    [0, 'rgba(74, 56, 112, 0.5)'],
    [0.38, 'rgba(74, 56, 112, 0.24)'],
    [0.72, 'rgba(74, 56, 112, 0.07)'],
    [1, 'rgba(74, 56, 112, 0)']
  ])
  const shadow = new THREE.Mesh(
    new THREE.CircleGeometry(0.42, 40),
    new THREE.MeshBasicMaterial({ map: shadowTex, transparent: true, depthWrite: false })
  )
  shadow.rotation.x = -Math.PI / 2
  shadow.position.y = 0.002
  ground.add(shadow)

  const ringTex = radialTexture([
    [0, 'rgba(150, 118, 226, 0)'],
    [0.58, 'rgba(150, 118, 226, 0)'],
    [0.76, 'rgba(138, 106, 220, 0.34)'],
    [0.9, 'rgba(96, 158, 235, 0.14)'],
    [1, 'rgba(96, 158, 235, 0)']
  ])
  const ring = new THREE.Mesh(
    new THREE.CircleGeometry(0.56, 48),
    new THREE.MeshBasicMaterial({ map: ringTex, transparent: true, depthWrite: false, opacity: 0.8 })
  )
  ring.rotation.x = -Math.PI / 2
  ground.add(ring)

  /* ---------- 灯光 ---------- */
  const ambient = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambient)

  const key = new THREE.DirectionalLight(0xfff2e4, 0.72)
  key.position.set(-1.6, 2.4, 3.2)
  scene.add(key)

  const fill = new THREE.DirectionalLight(0xbcd2ff, 0.3)
  fill.position.set(2.2, 0.4, 1.8)
  scene.add(fill)

  const rim = new THREE.PointLight(0xa78bfa, 6.5, 5.5, 2)
  rim.position.set(0.9, 0.55, -1.1)
  scene.add(rim)

  /* ---------- 明暗主题下的光照配比 ---------- */
  const THEME = {
    light: { ambient: 0.95, key: 0.5, fill: 0.22, rim: 4.2, emissive: 0.62, shadow: 0.72, ring: 0.6 },
    dark: { ambient: 0.52, key: 0.86, fill: 0.4, rim: 8.5, emissive: 0.44, shadow: 1, ring: 1 }
  }
  let themeKey = dark ? 'dark' : 'light'
  const cur = { ...THEME[themeKey] }

  /* ---------- 环绕微粒 ---------- */
  const SPARKS = 58
  const sparkGeo = new THREE.BufferGeometry()
  const sparkPos = new Float32Array(SPARKS * 3)
  const sparkSeed = new Float32Array(SPARKS * 3)
  for (let i = 0; i < SPARKS; i++) {
    const a = Math.random() * Math.PI * 2
    const r = 0.5 + Math.random() * 0.42
    sparkSeed[i * 3] = a
    sparkSeed[i * 3 + 1] = r
    sparkSeed[i * 3 + 2] = Math.random()
    sparkPos[i * 3] = Math.cos(a) * r
    sparkPos[i * 3 + 1] = GROUND_Y + 0.15 + Math.random() * 1.7
    sparkPos[i * 3 + 2] = Math.sin(a) * r * 0.5
  }
  sparkGeo.setAttribute('position', new THREE.BufferAttribute(sparkPos, 3))
  const sparkTex = radialTexture(
    [
      [0, 'rgba(255,255,255,0.92)'],
      [0.3, 'rgba(190,158,255,0.8)'],
      [1, 'rgba(150,180,255,0)']
    ],
    64
  )
  const sparkMat = new THREE.PointsMaterial({
    map: sparkTex,
    size: 0.05,
    sizeAttenuation: true,
    transparent: true,
    depthWrite: false,
    color: 0x8b6ee8
  })
  const sparks = new THREE.Points(sparkGeo, sparkMat)
  root.add(sparks)

  /* ---------- 状态 ---------- */
  const state = {
    action: startKey || poses[0].key,
    facing: 1,
    dragging: false,
    paused: reducedMotion,
    hover: false,
    mood: null,
    walkPhase: 0,
    bobPhase: 0,
    punch: 0
  }
  const rot = { y: 0, x: 0, z: 0 }
  const target = { y: 0, x: 0, z: 0, lift: 0, liftCur: 0, sway: 0 }
  const figures = new Map()

  const MOOD = {
    meteor: { rim: 0x38bdf8, spark: 0x2f86d8, ring: 0.95 },
    rainbow: { rim: 0xf472b6, spark: 0xe08a3c, ring: 1.0 },
    none: { rim: 0xa78bfa, spark: 0x8b6ee8, ring: 0.8 }
  }
  const moodCol = { rim: new THREE.Color(MOOD.none.rim), spark: new THREE.Color(MOOD.none.spark) }
  const moodTarget = { rim: new THREE.Color(MOOD.none.rim), spark: new THREE.Color(MOOD.none.spark) }
  let ringOpacity = MOOD.none.ring

  /* ---------- 贴图异步就绪 ---------- */
  let disposed = false
  Promise.all(
    poses.map(async (p) => {
      const img = await loadImage(p.src)
      const tex = new THREE.Texture(img)
      tex.colorSpace = THREE.SRGBColorSpace
      tex.generateMipmaps = true
      tex.minFilter = THREE.LinearMipmapLinearFilter
      tex.needsUpdate = true
      const built = buildFigure(img, tex, depthField(img))
      built.mesh.visible = false
      built.tex = tex
      figure.add(built.mesh)
      figures.set(p.key, built)
      applyAction()
    })
  ).catch(() => {})

  function applyAction() {
    figures.forEach((f, key) => {
      f.mesh.visible = key === state.action
    })
  }

  /* ---------- 循环 ---------- */
  let raf = 0
  let last = 0
  let clock = 0

  function frame(ts) {
    raf = requestAnimationFrame(frame)
    const dt = last ? Math.min((ts - last) / 1000, 0.05) : 0
    last = ts
    if (!state.paused) clock += dt
    render(dt)
  }

  function render(dt) {
    const moving = !state.paused

    if (moving) {
      state.bobPhase += dt * 1.9
      state.walkPhase += dt * 5.4
    }

    // 朝向：转向而非镜像，让体积真正转过来
    target.y = state.facing * 0.42 + (moving && !state.dragging ? Math.sin(state.walkPhase) * 0.07 : 0)
    target.x = state.dragging ? -0.2 : Math.sin(state.bobPhase * 0.5) * 0.018
    target.z = state.dragging ? 0.12 : Math.sin(state.bobPhase * 0.5 + 1) * 0.012
    target.lift = state.dragging ? 0.26 : 0

    rot.y = lerp(rot.y, target.y, 0.09)
    rot.x = lerp(rot.x, target.x, 0.1)
    rot.z = lerp(rot.z, target.z, 0.1)
    target.liftCur = lerp(target.liftCur, target.lift, 0.12)

    figure.rotation.set(rot.x, rot.y, rot.z)
    const breathe = moving ? 1 + Math.sin(state.bobPhase) * 0.012 : 1
    const hoverScale = state.hover && !state.dragging ? 1.06 : 1
    state.punch = lerp(state.punch, 0, 0.16)
    const punch = 1 + state.punch * 0.1
    figure.scale.set(hoverScale * punch, breathe * hoverScale * (1 - state.punch * 0.07), hoverScale * punch)
    figure.position.y = target.liftCur + (moving && !state.dragging ? Math.abs(Math.sin(state.walkPhase)) * 0.022 : 0)
    figure.position.z = target.liftCur * 0.5

    // 主题切换时平滑过渡光照
    const th = THEME[themeKey]
    cur.ambient = lerp(cur.ambient, th.ambient, 0.06)
    cur.key = lerp(cur.key, th.key, 0.06)
    cur.fill = lerp(cur.fill, th.fill, 0.06)
    cur.rim = lerp(cur.rim, th.rim, 0.06)
    cur.emissive = lerp(cur.emissive, th.emissive, 0.06)
    cur.shadow = lerp(cur.shadow, th.shadow, 0.06)
    cur.ring = lerp(cur.ring, th.ring, 0.06)
    ambient.intensity = cur.ambient
    key.intensity = cur.key
    fill.intensity = cur.fill
    figures.forEach((fig) => {
      fig.mesh.material.emissiveIntensity = cur.emissive
    })

    // 拖起来时阴影收小变淡，落地时撑回
    const gs = 1 - target.liftCur * 1.15
    ground.scale.setScalar(Math.max(0.55, gs))
    shadow.material.opacity = Math.max(0.16, (0.95 - target.liftCur * 2.1) * cur.shadow)
    ring.position.y = -target.liftCur * 0.35

    const mood = state.mood === 'meteor' ? MOOD.meteor : state.mood === 'rainbow' ? MOOD.rainbow : MOOD.none
    if (state.mood === 'rainbow' && moving) {
      const hue = (clock * 0.22) % 1
      moodCol.rim.setHSL(hue, 0.72, 0.58)
      moodCol.spark.setHSL((hue + 0.16) % 1, 0.72, 0.5)
    } else {
      moodTarget.rim.set(mood.rim)
      moodTarget.spark.set(mood.spark)
      moodCol.rim.lerp(moodTarget.rim, 0.06)
      moodCol.spark.lerp(moodTarget.spark, 0.06)
    }
    rim.color.copy(moodCol.rim)
    rim.intensity = cur.rim * (state.mood ? 1.35 : 1)
    sparkMat.color.copy(moodCol.spark)
    ringOpacity = lerp(ringOpacity, mood.ring * cur.ring, 0.06)
    ring.material.opacity = ringOpacity

    if (moving) {
      const spin = state.mood === 'meteor' ? 1.5 : 0.34
      const arr = sparkGeo.attributes.position.array
      for (let i = 0; i < SPARKS; i++) {
        const a = sparkSeed[i * 3] + clock * spin * (0.5 + sparkSeed[i * 3 + 2])
        const r = sparkSeed[i * 3 + 1]
        arr[i * 3] = Math.cos(a) * r
        arr[i * 3 + 1] += dt * (0.06 + sparkSeed[i * 3 + 2] * 0.09)
        arr[i * 3 + 2] = Math.sin(a) * r * 0.5
        if (arr[i * 3 + 1] > GROUND_Y + 2.05) arr[i * 3 + 1] = GROUND_Y + 0.1
      }
      sparkGeo.attributes.position.needsUpdate = true
      sparkMat.opacity = 0.55 + Math.sin(clock * 1.6) * 0.16
    }

    // 指针轻微视差
    camera.position.x = lerp(camera.position.x, parallax.x * 0.16, 0.05)
    camera.position.y = lerp(camera.position.y, 0.3 + parallax.y * 0.1, 0.05)
    camera.lookAt(0, -0.02, 0)

    renderer.render(scene, camera)
  }

  const parallax = { x: 0, y: 0 }
  function onPointerMove(e) {
    parallax.x = (e.clientX / window.innerWidth) * 2 - 1
    parallax.y = -((e.clientY / window.innerHeight) * 2 - 1)
  }

  function onResize() {
    const w = host.clientWidth
    const h = host.clientHeight
    if (!w || !h) return
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h, false)
  }

  function onVisibility() {
    if (document.hidden) {
      cancelAnimationFrame(raf)
      raf = 0
      last = 0
    } else if (!raf && !disposed) {
      raf = requestAnimationFrame(frame)
    }
  }

  const onContextLost = (e) => {
    e.preventDefault()
    cancelAnimationFrame(raf)
    raf = 0
  }

  raf = requestAnimationFrame(frame)

  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('resize', onResize, { passive: true })
  document.addEventListener('visibilitychange', onVisibility)
  renderer.domElement.addEventListener('webglcontextlost', onContextLost)

  return {
    setAction(key) {
      if (!figures.has(key) || state.action === key) return
      state.action = key
      state.punch = 1
      applyAction()
    },
    setFacing(n) {
      state.facing = n < 0 ? -1 : 1
    },
    setDragging(on) {
      state.dragging = !!on
    },
    setPaused(on) {
      state.paused = !!on || reducedMotion
    },
    setHover(on) {
      state.hover = !!on
    },
    setMood(m) {
      state.mood = m || null
    },
    setTheme(isDark) {
      themeKey = isDark ? 'dark' : 'light'
    },
    renderOnce() {
      render(0)
    },
    dispose() {
      if (disposed) return
      disposed = true
      cancelAnimationFrame(raf)
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('resize', onResize)
      document.removeEventListener('visibilitychange', onVisibility)
      renderer.domElement.removeEventListener('webglcontextlost', onContextLost)
      scene.traverse((obj) => {
        obj.geometry?.dispose()
        const mats = Array.isArray(obj.material) ? obj.material : obj.material ? [obj.material] : []
        mats.forEach((m) => {
          Object.keys(m).forEach((k) => {
            const v = m[k]
            if (v && v.isTexture) v.dispose()
          })
          m.dispose()
        })
      })
      sparkTex.dispose()
      renderer.dispose()
      renderer.forceContextLoss?.()
      if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement)
    }
  }
}
