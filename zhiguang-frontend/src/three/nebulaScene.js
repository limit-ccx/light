import * as THREE from 'three'
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js'
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js'

const clamp = (v, a, b) => Math.min(b, Math.max(a, v))
const dampFn = (cur, tgt, lambda, dt) => cur + (tgt - cur) * (1 - Math.exp(-lambda * dt))

const TAU = '6.28318530718'

const VERT = /* glsl */ `
#define TAU ${TAU}
#define PI 3.14159265359
attribute float aT; attribute vec4 aRnd;
uniform float uTime, uMorph, uTurb, uSize, uSpin, uDpr;
uniform vec3 uPointer, uColA, uColB, uColC;
uniform float uForce;
varying vec3 vCol; varying float vA;

vec3 sKnot(float t, vec4 r){
  float u = t * TAU;
  float r0 = 2.0 + cos(3.0 * u);
  vec3 p = vec3(r0 * cos(2.0 * u), r0 * sin(2.0 * u), -sin(3.0 * u)) * 1.15;
  return p + (r.xyz - 0.5) * 0.55;
}
vec3 sSphere(float t, vec4 r){
  float y = 1.0 - 2.0 * t;
  float rad = sqrt(max(0.0, 1.0 - y * y));
  float phi = t * 2.399963 * 320.0;
  float shell = 2.35 + (r.w - 0.5) * 0.42;
  return vec3(cos(phi) * rad, y, sin(phi) * rad) * shell + (r.xyz - 0.5) * 0.12;
}
vec3 sWave(float t, vec4 r){
  vec2 g = vec2(r.z, r.w);
  float x = (g.x - 0.5) * 6.6, z = (g.y - 0.5) * 6.6;
  float h = sin(x * 1.25 + uTime * 0.5) * cos(z * 1.15 - uTime * 0.4) * 0.72
          + sin(length(vec2(x, z)) * 1.6 - uTime * 0.8) * 0.3;
  return vec3(x, h + (r.x - 0.5) * 0.2, z);
}
vec3 sGalaxy(float t, vec4 r){
  float arm = floor(r.z * 3.0);
  float rad = pow(clamp(t, 0.0, 1.0), 0.55) * 3.5;
  float ang = rad * 1.35 + arm * TAU / 3.0 + (r.x - 0.5) * 0.55;
  float y = (r.y - 0.5) * 0.5 * exp(-rad * 0.55);
  return vec3(cos(ang) * rad, y, sin(ang) * rad);
}
vec3 sHelix(float t, vec4 r){
  float side = r.z < 0.5 ? 0.0 : PI;
  float u = t * TAU * 2.0 + side;
  float rad = 1.85 + (r.x - 0.5) * 0.35;
  vec3 p = vec3(cos(u) * rad, (t - 0.5) * 5.4, sin(u) * rad);
  float rung = step(0.86, r.w);
  p.xz = mix(p.xz, vec2(cos(u) * rad * (0.4 + r.w), sin(u) * rad * (0.4 + r.w)), rung);
  return p + (r.xyz - 0.5) * 0.14;
}
vec3 shapeAt(int i, float t, vec4 r){
  // 索引顺序即形态循环顺序：0 环面纽结 KNOT / 1 斐波那契球 SPHERE / 2 波面网格 WAVE / 3 银河旋臂 GALAXY / 4 双螺旋 HELIX
  if (i == 0) return sKnot(t, r);
  if (i == 1) return sSphere(t, r);
  if (i == 2) return sWave(t, r);
  if (i == 3) return sGalaxy(t, r);
  return sHelix(t, r);
}
void main(){
  vec3 pos = vec3(0.0);
  for (int i = 0; i < 5; i++) {
    float w = max(0.0, 1.0 - abs(uMorph - float(i)));
    if (w > 0.0005) pos += shapeAt(i, aT, aRnd) * w;
  }
  float tb = uTurb * (0.45 + aRnd.w);
  pos += tb * vec3(
    sin(pos.y * 1.6 + uTime * 0.7 + aRnd.x * TAU),
    cos(pos.z * 1.4 + uTime * 0.6 + aRnd.y * TAU),
    sin(pos.x * 1.8 + uTime * 0.8 + aRnd.z * TAU));

  float ang = uTime * uSpin * 0.16 * (1.25 - clamp(length(pos.xz) * 0.16, 0.0, 0.9));
  float ca = cos(ang), sa = sin(ang);
  pos = vec3(pos.x * ca - pos.z * sa, pos.y, pos.x * sa + pos.z * ca);

  vec3 d = pos - uPointer;
  float dd = dot(d, d);
  pos += normalize(d + vec3(1e-4)) * uForce * exp(-dd * 0.3) * 2.4;

  vec4 mv = modelViewMatrix * vec4(pos, 1.0);
  float dist = -mv.z;
  gl_PointSize = uSize * (0.55 + aRnd.x) * (24.0 / max(dist, 0.4)) * uDpr;
  gl_Position = projectionMatrix * mv;

  float rad = clamp(length(pos) / 3.4, 0.0, 1.0);
  vec3 c = mix(uColA, uColB, rad);
  c = mix(c, uColC, clamp(aRnd.y * 1.5 - 0.3, 0.0, 1.0) * 0.75);
  c += vec3(0.9) * pow(max(0.0, 1.0 - rad * 1.45), 2.6);
  vCol = c;
  float tw = 0.72 + 0.28 * sin(uTime * 1.9 + aRnd.x * 44.0);
  vA = (0.13 + 0.3 * aRnd.w) * tw * clamp(1.0 - (dist - 9.0) / 16.0, 0.15, 1.0);
}`

const FRAG = /* glsl */ `
varying vec3 vCol; varying float vA;
void main(){
  float d = length(gl_PointCoord - vec2(0.5));
  float a = smoothstep(0.5, 0.04, d);
  gl_FragColor = vec4(vCol * a * vA, a * vA);
}`

const STAR_VERT = /* glsl */ `
attribute vec4 aRnd;
uniform float uTime, uDpr;
varying float vA; varying vec3 vCol;
void main(){
  vec4 mv = modelViewMatrix * vec4(position, 1.0);
  gl_PointSize = (0.6 + aRnd.x * 1.4) * (60.0 / max(-mv.z, 1.0)) * uDpr;
  gl_Position = projectionMatrix * mv;
  vA = (0.55 + 0.45 * sin(uTime * 1.3 + aRnd.y * 30.0)) * (0.1 + 0.26 * aRnd.w);
  vCol = mix(vec3(0.5, 0.66, 1.0), vec3(0.9, 0.95, 1.0), aRnd.z);
}`

export function createNebulaScene({ canvas, initial = {}, isNarrow }) {
  const damp = dampFn
  const mix = (a, b, t) => a + (b - a) * t

  const state = {
    motion: initial.motion ?? true,
    turb: initial.turb ?? 0.3,
    size: initial.size ?? 1.6,
    bloom: initial.bloom ?? 0.9,
    spin: initial.spin ?? 0.35,
    count: initial.count ?? (isNarrow() ? 45000 : 120000),
    field: true,
    morphNow: 0,
    morphTarget: 0,
    morphHold: false,
    progress: 0,
    fps: 60,
    ms: 16.7,
    forceNow: 0,
    forceTarget: 0,
    autoQuality: true,
    degraded: false,
    slowmo: false
  }

  const renderer = new THREE.WebGLRenderer({
    canvas,
    antialias: false,
    alpha: false,
    powerPreference: 'high-performance'
  })
  renderer.setPixelRatio(Math.min(devicePixelRatio, isNarrow() ? 1.5 : 1.85))
  renderer.setSize(innerWidth, innerHeight)
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.05
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.info.autoReset = false

  const scene = new THREE.Scene()
  // 画布不透明，这层必须是深蓝而非近黑，否则参考图的夜空感会被吃掉
  scene.background = new THREE.Color('#061228')
  const camera = new THREE.PerspectiveCamera(52, innerWidth / innerHeight, 0.1, 200)
  camera.position.set(0, 1.3, 11.2)

  const U = {
    uTime: { value: 0 },
    uMorph: { value: 0 },
    uTurb: { value: state.turb },
    uSize: { value: state.size },
    uSpin: { value: state.spin },
    uDpr: { value: renderer.getPixelRatio() },
    uPointer: { value: new THREE.Vector3(999, 999, 999) },
    uForce: { value: 0 },
    uColA: { value: new THREE.Color('#3d6bff') },
    uColB: { value: new THREE.Color('#79d5ff') },
    uColC: { value: new THREE.Color('#b7a6ff') }
  }

  const cloudMat = new THREE.ShaderMaterial({
    vertexShader: VERT,
    fragmentShader: FRAG,
    uniforms: U,
    transparent: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    depthTest: false
  })

  let cloud = null
  let cloudGeo = null
  let starField = null

  function buildCloud(count) {
    const g = new THREE.BufferGeometry()
    const pos = new Float32Array(count * 3)
    const t = new Float32Array(count)
    const rnd = new Float32Array(count * 4)
    for (let i = 0; i < count; i++) {
      t[i] = i / (count - 1)
      rnd[i * 4] = Math.random()
      rnd[i * 4 + 1] = Math.random()
      rnd[i * 4 + 2] = Math.random()
      rnd[i * 4 + 3] = Math.random()
    }
    g.setAttribute('position', new THREE.BufferAttribute(pos, 3))
    g.setAttribute('aT', new THREE.BufferAttribute(t, 1))
    g.setAttribute('aRnd', new THREE.BufferAttribute(rnd, 4))
    g.boundingSphere = new THREE.Sphere(new THREE.Vector3(), 12)

    if (cloud) {
      cloudGeo.dispose()
      cloud.geometry = g
      cloudGeo = g
    } else {
      cloud = new THREE.Points(g, cloudMat)
      cloud.frustumCulled = false
      scene.add(cloud)
      cloudGeo = g
    }
    state.count = count
  }

  function buildStars() {
    const n = 2400
    const g = new THREE.BufferGeometry()
    const pos = new Float32Array(n * 3)
    const rnd = new Float32Array(n * 4)
    const v = new THREE.Vector3()
    for (let i = 0; i < n; i++) {
      v.randomDirection().multiplyScalar(26 + Math.random() * 36)
      pos.set([v.x, v.y, v.z], i * 3)
      rnd.set([Math.random(), Math.random(), Math.random(), Math.random()], i * 4)
    }
    g.setAttribute('position', new THREE.BufferAttribute(pos, 3))
    g.setAttribute('aRnd', new THREE.BufferAttribute(rnd, 4))
    starField = new THREE.Points(
      g,
      new THREE.ShaderMaterial({
        vertexShader: STAR_VERT,
        fragmentShader: FRAG,
        uniforms: { uTime: U.uTime, uDpr: U.uDpr },
        transparent: true,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
        depthTest: false
      })
    )
    starField.frustumCulled = false
    scene.add(starField)
  }

  const composer = new EffectComposer(renderer)
  composer.addPass(new RenderPass(scene, camera))
  const bloom = new UnrealBloomPass(new THREE.Vector2(innerWidth, innerHeight), state.bloom, 0.62, 0.3)
  composer.addPass(bloom)
  composer.addPass(new OutputPass())

  buildCloud(state.count)
  buildStars()

  /* ---------- 相机 / 指针力场 ---------- */
  const mouse = { x: 0, y: 0 }
  const camTarget = new THREE.Vector3()
  const camAim = new THREE.Vector3(1.95, 0, 0)
  const ndc = new THREE.Vector3()
  const _pw = new THREE.Vector3()
  let pressed = false

  function stepCamera(dt) {
    const m = state.motion ? 1 : 0
    const aimX = isNarrow() ? 0.2 : 1.95
    camTarget.set(
      mouse.x * 2.1 * m + Math.sin(state.progress * 2.3) * 0.9 * m + aimX * 0.55,
      1.3 - state.progress * 1.2 + mouse.y * -1 * m,
      11.2 - state.progress * 2.3
    )
    camAim.x = damp(camAim.x, aimX, 3, dt)
    camera.position.lerp(camTarget, 1 - Math.exp(-2.4 * dt))
    camera.lookAt(camAim.x, 0, 0)
  }

  function pointerToWorld(cx, cy, out) {
    ndc.set((cx / innerWidth) * 2 - 1, -(cy / innerHeight) * 2 + 1, 0.5).unproject(camera)
    ndc.sub(camera.position).normalize()
    const t = -camera.position.z / (ndc.z || -1e-4)
    return out.copy(camera.position).addScaledVector(ndc, clamp(t, 1, 60))
  }

  function degrade() {
    state.degraded = true
    renderer.setPixelRatio(1)
    U.uDpr.value = 1
    renderer.setSize(innerWidth, innerHeight)
    composer.setSize(innerWidth, innerHeight)
    bloom.strength = 0
    state.bloom = 0
    buildCloud(Math.round(state.count * 0.5))
  }

  /* ---------- 主循环 ---------- */
  let raf = 0
  let last = performance.now()
  let slowT = 0

  function tick(now) {
    raf = requestAnimationFrame(tick)
    const dt = Math.min((now - last) / 1000, 0.05)
    last = now
    const sc = state.slowmo ? 0.35 : 1
    state.ms = mix(state.ms, dt * 1000, 0.12)
    state.fps = mix(state.fps, 1 / Math.max(dt, 1e-4), 0.1)

    U.uTime.value += dt * sc
    if (!state.morphHold) state.morphNow = damp(state.morphNow, state.morphTarget, 6, dt * sc)
    U.uMorph.value = state.morphNow

    stepCamera(dt)
    state.forceNow = damp(state.forceNow, state.field ? state.forceTarget : 0, 5.5, dt)
    U.uForce.value = state.forceNow

    if (state.autoQuality && !state.degraded) {
      slowT += state.fps < 24 ? dt : -dt * 1.5
      if (slowT > 3) degrade()
    }

    if (!document.hidden) {
      renderer.info.reset()
      composer.render()
    }
  }

  const onVisibility = () => {
    if (!document.hidden) last = performance.now()
  }
  document.addEventListener('visibilitychange', onVisibility)

  return {
    state,
    setTurb: (v) => { state.turb = v; U.uTurb.value = v },
    setSize: (v) => { state.size = v; U.uSize.value = v },
    setBloom: (v) => { state.bloom = v; bloom.strength = v },
    setSpin: (v) => { state.spin = v; U.uSpin.value = v },
    setMotion: (on) => { state.motion = on },
    setMorph: (v, hold = true) => { state.morphHold = hold; state.morphNow = v; U.uMorph.value = v },
    setPointer: (cx, cy) => {
      mouse.x = clamp((cx / innerWidth) * 2 - 1, -1, 1)
      mouse.y = clamp((cy / innerHeight) * 2 - 1, -1, 1)
      if (state.field) {
        state.forceTarget = pressed ? 2.6 : 0.55
        U.uPointer.value.copy(pointerToWorld(cx, cy, _pw))
      }
    },
    setPressed: (on) => {
      pressed = on
      if (state.field) state.forceTarget = on ? 2.6 : 0.55
    },
    clearForce: () => { state.forceTarget = 0 },
    compile: () => { renderer.compile(scene, camera); composer.render() },
    start: () => { if (!raf) { last = performance.now(); raf = requestAnimationFrame(tick) } },
    resize: () => {
      renderer.setPixelRatio(Math.min(devicePixelRatio, isNarrow() ? 1.5 : 1.85))
      U.uDpr.value = renderer.getPixelRatio()
      renderer.setSize(innerWidth, innerHeight)
      composer.setSize(innerWidth, innerHeight)
      bloom.setSize(innerWidth, innerHeight)
      camera.aspect = innerWidth / innerHeight
      camera.updateProjectionMatrix()
    },
    dispose: () => {
      cancelAnimationFrame(raf)
      document.removeEventListener('visibilitychange', onVisibility)
      scene.traverse((obj) => {
        obj.geometry?.dispose()
        const mats = Array.isArray(obj.material) ? obj.material : obj.material ? [obj.material] : []
        mats.forEach((m) => m.dispose())
      })
      composer.passes?.forEach((p) => p.dispose?.())
      composer.dispose?.()
      renderer.dispose()
      renderer.forceContextLoss?.()
    }
  }
}
