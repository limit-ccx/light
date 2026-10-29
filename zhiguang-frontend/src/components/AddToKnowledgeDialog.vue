<template>
  <el-dialog
    v-model="visible"
    title="添加到知识库"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="选择分类">
        <el-cascader
          v-model="form.categoryId"
          :options="categoryOptions"
          :props="{ value: 'id', label: 'name', checkStrictly: true, emitPath: false }"
          clearable
          placeholder="选择分类（可选）"
          style="width: 100%"
        />
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
      <el-button type="primary" @click="handleAdd" :loading="loading">
        添加到知识库
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addToKnowledge, getCategories, getAllTags } from '@/api/knowledge'

const props = defineProps({
  modelValue: Boolean,
  articleId: Number
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(false)
const loading = ref(false)
const categoryOptions = ref([])
const existingTags = ref([])

const form = reactive({
  articleId: null,
  categoryId: null,
  notes: '',
  customTags: []
})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    form.articleId = props.articleId
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
  form.articleId = null
  form.categoryId = null
  form.notes = ''
  form.customTags = []
}

const loadCategories = async () => {
  try {
    const categories = await getCategories()
    categoryOptions.value = categories || []
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

const handleAdd = async () => {
  if (!form.articleId) {
    ElMessage.warning('文章ID不能为空')
    return
  }

  loading.value = true
  try {
    await addToKnowledge({
      articleId: form.articleId,
      categoryId: form.categoryId,
      notes: form.notes,
      customTags: form.customTags
    })
    ElMessage.success('已添加到知识库')
    visible.value = false
    emit('success')
  } catch (error) {
    const message = error.response?.data?.message || '添加失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>
