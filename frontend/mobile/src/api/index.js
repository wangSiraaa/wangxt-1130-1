import request from '../utils/request'

export const api = {
  login: (data) => request.post('/common/login', data),
  getDashboard: () => request.get('/common/dashboard').then(r => r.data),
  listPlots: () => request.get('/common/plots').then(r => r.data),
  listUsers: (role) => request.get('/common/users', { params: { roleCode: role } }).then(r => r.data),

  queryPrescriptions: (params) => request.get('/prescription/page', { params }).then(r => r.data),
  getPrescription: (id) => request.get(`/prescription/${id}`).then(r => r.data),
  createPrescription: (data) => request.post('/prescription/create', data).then(r => r.data),
  submitPrescription: (id, params) => request.post(`/prescription/submit/${id}`, null, { params }).then(r => r.data),

  queryOutbounds: (params) => request.get('/outbound/page', { params }).then(r => r.data),
  getOutbound: (id) => request.get(`/outbound/${id}`).then(r => r.data),
  getPendingOperations: (pilotId) => request.get(`/outbound/pending-operations/${pilotId}`).then(r => r.data),

  preCheckFlight: (params) => request.post('/flight/pre-check', null, { params }).then(r => r.data),
  createFlight: (data) => request.post('/flight/create', data).then(r => r.data),
  completeFlight: (id, data) => request.post(`/flight/complete/${id}`, data).then(r => r.data),
  queryFlights: (params) => request.get('/flight/page', { params }).then(r => r.data),
  getPlotWarnings: () => request.get('/flight/plot-warnings').then(r => r.data),
  queryReminders: (params) => request.get('/flight/reminders', { params }).then(r => r.data),
  markReminderRead: (id) => request.post(`/flight/reminders/${id}/read`).then(r => r),

  reassignPilot: (id, params) => request.post(`/flight/reassign-pilot/${id}`, null, { params }).then(r => r.data),
  cancelFlightDueToWeather: (id, params) => request.post(`/flight/cancel-weather/${id}`, null, { params }).then(r => r.data),

  validatePrescriptionComprehensive: (data) => request.post('/prescription/validate-comprehensive', data).then(r => r.data),

  queryHarvestPlans: (params) => request.get('/harvest-plan/page', { params }).then(r => r.data),
  completeHarvest: (id) => request.post(`/harvest-plan/complete/${id}`, { operatorId: JSON.parse(localStorage.getItem('m_user') || '{}').id }).then(r => r.data)
}
