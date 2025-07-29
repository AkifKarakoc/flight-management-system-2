<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <slot :form="form" />
    </el-form>

    <template #footer>
      <el-button @click="handleClose">İptal</el-button>
      <el-button
        type="primary"
        :loading="loading"
        @click="handleSubmit"
      >
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, required: true },
  form: { type: Object, required: true },
  rules: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  width: { type: String, default: '600px' },
  submitText: { type: String, default: 'Kaydet' }
})

const emit = defineEmits(['update:modelValue', 'submit', 'close'])

const formRef = ref()
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    emit('submit')
  } catch (error) {
    console.log('Validation failed')
  }
}

const handleClose = () => {
  emit('close')
  visible.value = false
}
</script>
