<template>
  <el-dialog
    v-model="visible"
    title="导入本地文档"
    width="600px"
    :close-on-click-modal="false"
  >
    <el-form :model="form" label-width="80px">
      <!-- 文件上传 -->
      <el-form-item label="选择文件" required>
        <el-upload
          ref="uploadRef"
          class="upload-area"
          drag
          action=""
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :limit="1"
          accept=".txt,.md,.markdown,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.html,.htm"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持格式：txt, md, pdf, doc, docx, xls, xlsx, ppt, pptx, html
            </div>
          </template>
        </el-upload>
      </el-form-item>

      <!-- 文件信息预览 -->
      <el-form-item label="文档标题" v-if="parsedTitle">
        <el-input v-model="form.title" placeholder="文档标题" />
      </el-form-item>

      <el-form-item label="内容预览" v-if="parsedContent">
        <el-input
          v-model="parsedContentPreview"
          type="textarea"
          :rows="5"
          readonly
          placeholder="文档内容预览..."
        />
      </el-form-item>

      <el-form-item label="选择分类">
        <el-select v-model="form.categoryId" clearable placeholder="选择分类（可选）" style="width: 100%">
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="添加标签">
        <el-select
          v-model="form.customTags"
          multiple
          filterable
          allow-create
          placeholder="选择或创建标签"
          style="width: 100%"
        >
          <el-option
            v-for="tag in existingTags"
            :key="tag"
            :label="tag"
            :value="tag"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="个人笔记">
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="3"
          placeholder="添加个人笔记（可选）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleImport" :loading="loading" :disabled="!selectedFile">
        导入文档
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { importDocument, getCategories, getAllTags } from '@/api/knowledge'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(false)
const loading = ref(false)
const uploadRef = ref(null)
const selectedFile = ref(null)
const categories = ref([])
const existingTags = ref([])
const parsedTitle = ref('')
const parsedContent = ref('')

const parsedContentPreview = computed(() => {
  if (!parsedContent.value) return ''
  return parsedContent.value.length > 500
    ? parsedContent.value.substring(0, 500) + '...'
    : parsedContent.value
})

const form = reactive({
  title: '',
  categoryId: null,
  notes: '',
  customTags: []
})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    loadCategories()
    loadTags()
  }
})

watch(() => visible.value, (val) => {
  emit('update:modelValue', val)
  if (!val) {
    resetForm()
  }
})

const resetForm = () => {
  selectedFile.value = null
  parsedTitle.value = ''
  parsedContent.value = ''
  form.title = ''
  form.categoryId = null
  form.notes = ''
  form.customTags = []
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

const handleFileChange = (uploadFile) => {
  selectedFile.value = uploadFile.raw
  // 使用文件名作为默认标题
  const fileName = uploadFile.name
  const titleWithoutExt = fileName.replace(/\.[^/.]+$/, '')
  parsedTitle.value = titleWithoutExt
  form.title = titleWithoutExt

  // 读取文本文件内容预览
  const file = uploadFile.raw
  if (file.type === 'text/plain' || file.type === 'text/markdown' || file.name.endsWith('.txt') || file.name.endsWith('.md')) {
    const reader = new FileReader()
    reader.onload = (e) => {
      parsedContent.value = e.target.result
    }
    reader.readAsText(file)
  } else {
    parsedContent.value = `[${file.type || '未知类型'} 文件，内容将在上传后解析]`
  }
}

const handleFileRemove = () => {
  selectedFile.value = null
  parsedTitle.value = ''
  parsedContent.value = ''
  form.title = ''
}

const loadCategories = async () => {
  try {
    const result = await getCategories()
    categories.value = result || []
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadTags = async () => {
  try {
    const tags = await getAllTags()
    existingTags.value = tags || []
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

const handleImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    if (form.categoryId) {
      formData.append('categoryId', form.categoryId)
    }
    if (form.notes) {
      formData.append('notes', form.notes)
    }
    if (form.customTags && form.customTags.length > 0) {
      form.customTags.forEach(tag => {
        formData.append('customTags', tag)
      })
    }

    await importDocument(formData)
    ElMessage.success('文档导入成功')
    visible.value = false
    emit('success')
  } catch (error) {
    const message = error.response?.data?.message || '导入失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.upload-area {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
}
</style>
