import request from '../utils/request'

export const api = {
  login: (data) => request.post('/common/login', data),
  getDashboard: () => request.get('/common/dashboard').then(r => r.data),

  listPlots: (params) => request.get('/common/plots', { params }).then(r => r.data),
  listPesticides: (params) => request.get('/common/pesticides', { params }).then(r => r.data),
  listUsers: (roleCode) => request.get('/common/users', { params: { roleCode } }).then(r => r.data),
  queryStocks: (params) => request.get('/common/stocks', { params }).then(r => r.data),
  listIntervalConfigs: (params) => request.get('/common/interval-configs', { params }).then(r => r.data),

  createPrescription: (data) => request.post('/prescription/create', data).then(r => r.data),
  submitPrescription: (id, params) => request.post(`/prescription/submit/${id}`, null, { params }).then(r => r.data),
  approvePrescription: (id, params) => request.post(`/prescription/approve/${id}`, null, { params }).then(r => r.data),
  getPrescription: (id) => request.get(`/prescription/${id}`).then(r => r.data),
  queryPrescriptions: (params) => request.get('/prescription/page', { params }).then(r => r.data),
  getApprovedPrescriptions: () => request.get('/prescription/approved/list').then(r => r.data),

  createOutbound: (params) => request.post('/outbound/create-from-prescription', null, { params }).then(r => r.data),
  confirmOutbound: (outboundId, data) => request.post(`/outbound/confirm/${outboundId}`, data).then(r => r.data),
  getOutbound: (id) => request.get(`/outbound/${id}`).then(r => r.data),
  queryOutbounds: (params) => request.get('/outbound/page', { params }).then(r => r.data),
  getPendingOperations: (pilotId) => request.get(`/outbound/pending-operations/${pilotId}`).then(r => r.data),
  getPesticideStock: (pesticideId) => request.get(`/outbound/stock/${pesticideId}`).then(r => r.data),

  preCheckFlight: (params) => request.post('/flight/pre-check', null, { params }).then(r => r.data),
  createFlight: (data) => request.post('/flight/create', data).then(r => r.data),
  completeFlight: (id, data) => request.post(`/flight/complete/${id}`, data).then(r => r.data),
  getFlight: (id) => request.get(`/flight/${id}`).then(r => r.data),
  queryFlights: (params) => request.get('/flight/page', { params }).then(r => r.data),
  queryReminders: (params) => request.get('/flight/reminders', { params }).then(r => r.data),
  getPlotWarnings: (plotId) => request.get(`/flight/plot-warnings/${plotId || ''}`).then(r => r.data),
  markReminderRead: (id, params) => request.post(`/flight/reminders/${id}/read`, null, { params }).then(r => r),

  validatePrescriptionComprehensive: (data) => request.post('/prescription/validate-comprehensive', data).then(r => r.data),

  reassignPilot: (id, params) => request.post(`/flight/reassign-pilot/${id}`, null, { params }).then(r => r.data),
  cancelFlightDueToWeather: (id, params) => request.post(`/flight/cancel-weather/${id}`, null, { params }).then(r => r.data),

  cancelFlightToPending: (outboundId, params) => request.post(`/outbound/cancel-flight/${outboundId}`, null, { params }).then(r => r.data),
  queryPendingStock: (params) => request.get('/outbound/pending-stock/page', { params }).then(r => r.data),
  verifyPendingStock: (id, params) => request.post(`/outbound/pending-stock/${id}/verify`, null, { params }).then(r => r),

  createHarvestPlan: (data) => request.post('/harvest-plan/create', data).then(r => r.data),
  getHarvestPlan: (id) => request.get(`/harvest-plan/${id}`).then(r => r.data),
  queryHarvestPlans: (params) => request.get('/harvest-plan/page', { params }).then(r => r.data),
  completeHarvest: (id, params) => request.post(`/harvest-plan/complete/${id}`, null, { params }).then(r => r.data),
  cancelHarvest: (id) => request.post(`/harvest-plan/cancel/${id}`).then(r => r.data),
  unlockExpiredPlans: () => request.post('/harvest-plan/unlock-expired').then(r => r)
}
