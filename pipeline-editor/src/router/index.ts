import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import PipelineList from '@/views/PipelineList.vue'
import PipelineDetail from '@/views/PipelineDetail.vue'
import Config from '@/views/Config.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    { path: '/pipelines', component: PipelineList },
    { path: '/pipelineDetail/:id?', component: PipelineDetail },
    { path: '/config', component: Config },
  ]
})

export default router