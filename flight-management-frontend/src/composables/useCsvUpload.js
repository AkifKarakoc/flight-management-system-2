import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { flightAPI } from '@/services/api'

export const useCsvUpload = () => {
    const uploading = ref(false)
    const fileList = ref([])
    const previewData = ref(null)

    const handleFileSelect = (file) => {
        fileList.value = [file]
        previewData.value = null
    }

    const previewCsv = async () => {
        if (!fileList.value.length) {
            ElMessage.error('Dosya seçin')
            return null
        }

        uploading.value = true
        try {
            const formData = new FormData()
            formData.append('file', fileList.value[0].raw)

            const preview = await flightAPI.previewCSV(formData)
            previewData.value = preview

            if (!preview.readyForImport) {
                ElMessage.warning(`${preview.invalidRows} geçersiz satır`)
            }

            return preview
        } catch (error) {
            ElMessage.error('Önizleme hatası')
            throw error
        } finally {
            uploading.value = false
        }
    }

    const uploadCsv = async (onSuccess) => {
        if (!previewData.value) {
            const preview = await previewCsv()
            if (!preview?.readyForImport) return
        }

        uploading.value = true
        try {
            const validRows = previewData.value.previewData.filter(row => row.valid)
            const result = await flightAPI.confirmCSVUpload(validRows)

            ElMessage.success(`${result.successCount} uçuş yüklendi`)

            // Reset state
            fileList.value = []
            previewData.value = null

            onSuccess?.()
        } catch (error) {
            ElMessage.error('Yükleme hatası')
            throw error
        } finally {
            uploading.value = false
        }
    }

    const reset = () => {
        fileList.value = []
        previewData.value = null
        uploading.value = false
    }

    return {
        uploading,
        fileList,
        previewData,
        handleFileSelect,
        previewCsv,
        uploadCsv,
        reset
    }
}