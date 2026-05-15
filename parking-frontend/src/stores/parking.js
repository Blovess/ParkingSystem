import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useParkingStore = defineStore('parking', () => {
  const selectedSpaceId = ref(null)
  const pendingPlateNumber = ref('')
  const pendingRecordId = ref(null)
  const refreshKey = ref(0)

  function setSelectedSpaceId(id) {
    selectedSpaceId.value = id
  }

  function setPendingPlateNumber(plate) {
    pendingPlateNumber.value = plate
  }

  function setPendingRecordId(recordId) {
    pendingRecordId.value = recordId
  }

  function clearEntryState() {
    selectedSpaceId.value = null
    pendingPlateNumber.value = ''
    pendingRecordId.value = null
  }

  function triggerRefresh() {
    refreshKey.value++
  }

  return { selectedSpaceId, pendingPlateNumber, pendingRecordId, refreshKey, setSelectedSpaceId, setPendingPlateNumber, setPendingRecordId, clearEntryState, triggerRefresh }
})
