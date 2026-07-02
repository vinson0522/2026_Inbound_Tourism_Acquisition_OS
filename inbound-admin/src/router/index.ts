import { createWebHistory, createRouter, RouteRecordRaw } from 'vue-router';
/* Layout */
import Layout from '@/layout/index.vue';

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/social-callback',
    hidden: true,
    component: () => import('@/layout/components/SocialCallback/index.vue')
  },
  {
    path: '/login',
    component: () => import('@/views/login.vue'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register.vue'),
    hidden: true
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401.vue'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        component: () => import('@/views/tourgeo/dashboard/index.vue'),
        name: 'TourgeoDashboard',
        meta: { title: '工作台', icon: 'dashboard', affix: true }
      },
      {
        path: '/index',
        redirect: '/dashboard',
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard' },
        hidden: true
      }
    ]
  },
  {
    path: '/projects',
    component: Layout,
    redirect: '/projects/index',
    name: 'TourgeoProjects',
    meta: { title: '客户项目', icon: 'list' },
    alwaysShow: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/tourgeo/projects/index.vue'),
        name: 'ProjectList',
        meta: { title: '项目列表', icon: 'list' }
      },
      {
        path: ':projectId',
        component: () => import('@/views/tourgeo/projects/detail.vue'),
        name: 'ProjectDetail',
        hidden: true,
        meta: { title: '项目详情', activeMenu: '/projects/index', noCache: true }
      },
      {
        path: ':projectId/keywords',
        component: () => import('@/views/tourgeo/keywords/index.vue'),
        name: 'ProjectKeywords',
        hidden: true,
        meta: { title: '机会词列表', activeMenu: '/keywords/index', noCache: true }
      },
      {
        path: ':projectId/content-tasks',
        component: () => import('@/views/tourgeo/content/index.vue'),
        name: 'ProjectContentTasks',
        hidden: true,
        meta: { title: '内容任务', activeMenu: '/content-tasks/index', noCache: true }
      },
      {
        path: ':projectId/landing-pages',
        component: () => import('@/views/tourgeo/landing/index.vue'),
        name: 'ProjectLandingPages',
        hidden: true,
        meta: { title: '页面草稿', activeMenu: '/landing-pages/index', noCache: true }
      },
      {
        path: ':projectId/leads',
        component: () => import('@/views/tourgeo/leads/index.vue'),
        name: 'ProjectLeads',
        hidden: true,
        meta: { title: '询盘线索', activeMenu: '/leads/index', noCache: true }
      },
      {
        path: ':projectId/reports',
        component: () => import('@/views/tourgeo/reports/index.vue'),
        name: 'ProjectReports',
        hidden: true,
        meta: { title: '报告列表', activeMenu: '/reports/index', noCache: true }
      },
      {
        path: ':projectId/materials',
        component: () => import('@/views/tourgeo/materials/index.vue'),
        name: 'ProjectMaterials',
        hidden: true,
        meta: { title: '爆款拆解', activeMenu: '/content-tasks/materials', noCache: true }
      }
    ]
  },
  {
    path: '/keywords',
    component: Layout,
    redirect: '/keywords/index',
    name: 'TourgeoKeywords',
    meta: { title: '关键词洞察', icon: 'search' },
    alwaysShow: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/tourgeo/keywords/index.vue'),
        name: 'KeywordsList',
        meta: { title: '机会词列表', icon: 'list' }
      }
    ]
  },
  {
    path: '/content-tasks',
    component: Layout,
    redirect: '/content-tasks/index',
    name: 'TourgeoContent',
    meta: { title: '内容 Agent', icon: 'video-camera' },
    alwaysShow: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/tourgeo/content/index.vue'),
        name: 'ContentTasksList',
        meta: { title: '内容任务', icon: 'list' }
      },
      {
        path: 'materials',
        component: () => import('@/views/tourgeo/materials/index.vue'),
        name: 'MaterialsList',
        meta: { title: '爆款拆解', icon: 'video-play' }
      }
    ]
  },
  {
    path: '/landing-pages',
    component: Layout,
    redirect: '/landing-pages/index',
    name: 'TourgeoLanding',
    meta: { title: '落地页 Agent', icon: 'link' },
    alwaysShow: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/tourgeo/landing/index.vue'),
        name: 'LandingPagesList',
        meta: { title: '页面草稿', icon: 'list' }
      }
    ]
  },
  {
    path: '/leads',
    component: Layout,
    redirect: '/leads/index',
    name: 'TourgeoLeads',
    meta: { title: '线索与转化', icon: 'message' },
    alwaysShow: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/tourgeo/leads/index.vue'),
        name: 'LeadsList',
        meta: { title: '询盘线索', icon: 'list' }
      }
    ]
  },
  {
    path: '/reports',
    component: Layout,
    redirect: '/reports/index',
    name: 'TourgeoReports',
    meta: { title: '报告中心', icon: 'documentation' },
    alwaysShow: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/tourgeo/reports/index.vue'),
        name: 'ReportsList',
        meta: { title: '报告列表', icon: 'list' }
      }
    ]
  },
  {
    path: '/diagnostics',
    component: Layout,
    redirect: '/diagnostics/runs',
    name: 'TourgeoDiagnostics',
    meta: { title: 'GEO 诊断', icon: 'chart' },
    alwaysShow: true,
    children: [
      {
        path: 'runs',
        component: () => import('@/views/tourgeo/diagnostics/index.vue'),
        name: 'DiagnosticRuns',
        meta: { title: '诊断任务', icon: 'list' }
      },
      {
        path: 'runs/:runId',
        component: () => import('@/views/tourgeo/diagnostics/detail.vue'),
        name: 'DiagnosticDetail',
        hidden: true,
        meta: { title: '诊断详情', activeMenu: '/diagnostics/runs', noCache: true }
      },
      {
        path: 'trends',
        component: () => import('@/views/tourgeo/diagnostics/trends.vue'),
        name: 'DiagnosticTrends',
        meta: { title: '趋势监控', icon: 'chart' }
      }
    ]
  },
  {
    path: '/settings',
    component: Layout,
    redirect: '/settings/billing',
    name: 'TourgeoSettings',
    meta: { title: '系统设置', icon: 'system' },
    alwaysShow: true,
    children: [
      {
        path: 'billing',
        component: () => import('@/views/tourgeo/settings/billing/index.vue'),
        name: 'BillingSettings',
        meta: { title: '套餐与额度', icon: 'money' }
      },
      {
        path: 'report-template',
        component: () => import('@/views/tourgeo/settings/report-template/index.vue'),
        name: 'ReportTemplateSettings',
        meta: { title: '报告模板', icon: 'documentation' }
      },
      {
        path: 'probe-nodes',
        component: () => import('@/views/tourgeo/settings/probe-nodes/index.vue'),
        name: 'ProbeNodesSettings',
        meta: { title: '探针节点', icon: 'monitor' }
      },
      {
        path: 'probe-adapters',
        component: () => import('@/views/tourgeo/settings/probe-adapters/index.vue'),
        name: 'ProbeAdaptersSettings',
        meta: { title: '平台 Adapter', icon: 'component' }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index.vue'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
];

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes: RouteRecordRaw[] = [

];

/**
 * 创建路由
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_APP_CONTEXT_PATH),
  routes: constantRoutes,
  // 刷新时，滚动条位置还原
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    return { top: 0 };
  }
});

export default router;
