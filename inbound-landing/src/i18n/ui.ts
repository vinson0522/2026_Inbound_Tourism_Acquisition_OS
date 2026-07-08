export type Lang = 'en' | 'zh';

export const DEFAULT_LANG: Lang = 'en';
export const LANGS: Lang[] = ['en', 'zh'];

export interface NavCopy {
  features: string;
  pricing: string;
  contact: string;
  login: string;
  bookDemo: string;
}

export interface FooterCopy {
  privacy: string;
  terms: string;
  disclaimer: string;
  poweredBy: string;
  rights: string;
}

export interface ValueCard {
  icon: string;
  title: string;
  body: string;
}

export interface CapabilityCard {
  icon: string;
  title: string;
  points: string[];
}

export interface PricingTier {
  name: string;
  price: string;
  period: string;
  tagline: string;
  features: string[];
  cta: string;
  featured?: boolean;
}

export interface ContactField {
  label: string;
  placeholder?: string;
}

export interface Dictionary {
  htmlLang: string;
  meta: { title: string; description: string };
  nav: NavCopy;
  footer: FooterCopy;
  hero: {
    subZh: string;
    title: string;
    lead: string;
    compliance: string;
    bookDemo: string;
    login: string;
  };
  value: { title: string; cards: ValueCard[] };
  capabilities: { title: string; cards: CapabilityCard[] };
  workflow: { steps: string[] };
  cta: { title: string; body: string; bookDemo: string; login: string };
  pricing: {
    metaTitle: string;
    metaDescription: string;
    heading: string;
    subheading: string;
    tiers: PricingTier[];
    note: string;
    faqTitle: string;
    faqs: { q: string; a: string }[];
  };
  contact: {
    metaTitle: string;
    metaDescription: string;
    heading: string;
    subheading: string;
    fields: {
      name: ContactField;
      email: ContactField;
      phone: ContactField;
      company: ContactField;
      message: ContactField;
    };
    submit: string;
    submitting: string;
    hint: string;
    turnstileDisabled: string;
    errRequired: string;
    errContact: string;
    errCaptcha: string;
    errNetwork: string;
    errGeneric: string;
    success: string;
  };
}

const en: Dictionary = {
  htmlLang: 'en',
  meta: {
    title: 'TourGEO — AI Growth for China Inbound Tourism',
    description:
      'GEO visibility diagnostics, keyword insights, and AI content for tour operators targeting US, UK, and Australia travelers.',
  },
  nav: {
    features: 'Features',
    pricing: 'Pricing',
    contact: 'Contact',
    login: 'Log in',
    bookDemo: 'Book a demo',
  },
  footer: {
    privacy: 'Privacy',
    terms: 'Terms',
    disclaimer:
      'GEO and AI outputs are sampled at a specific time and platform set; not a guarantee of future visibility. Prices and visa information on generated pages require human confirmation.',
    poweredBy: 'Powered by TourGEO Agent',
    rights: 'TourGEO / 旅获 AI',
  },
  hero: {
    subZh: '旅获 AI · 入境游海外获客增长系统',
    title: 'Win overseas travelers where they ask AI',
    lead: 'GEO visibility diagnostics, keyword insights, and AI-assisted content & landing pages — built for China inbound tour operators.',
    compliance: 'Sample-based AI visibility insights. No ranking guarantees.',
    bookDemo: 'Book a demo',
    login: 'Log in to Admin',
  },
  value: {
    title: 'Why tour operators choose TourGEO',
    cards: [
      {
        icon: '📊',
        title: 'Measure',
        body: 'See how often AI assistants mention your brand vs competitors across real traveler questions.',
      },
      {
        icon: '⚡',
        title: 'Act',
        body: 'Prioritize keyword opportunities and generate review-ready scripts and English landing copy.',
      },
      {
        icon: '📥',
        title: 'Convert',
        body: 'Publish compliant landing pages with lead forms and WhatsApp handoff — track inquiries in one place.',
      },
    ],
  },
  capabilities: {
    title: 'Everything you need for inbound growth',
    cards: [
      {
        icon: '🌐',
        title: 'GEO Diagnostics',
        points: [
          'Grounded AI sampling across Perplexity, Gemini, OpenAI',
          'Visibility score with citation coverage',
          'Scheduled re-runs for US, UK, AU markets',
        ],
      },
      {
        icon: '🔑',
        title: 'Keyword Intelligence',
        points: [
          'Eight-stage traveler journey mapping',
          'Opportunity scoring for overseas demand',
          'Export insights straight into content workflows',
        ],
      },
      {
        icon: '🎬',
        title: 'Content Agent',
        points: [
          'Social scripts and storyboards in English',
          'RAG from your knowledge base with source citations',
          'Human review flag on every generated asset',
        ],
      },
      {
        icon: '📄',
        title: 'Landing Page Agent',
        points: [
          'English SSG pages aligned to keyword intent',
          'Turnstile lead forms and WhatsApp handoff',
          'Publish and preview in one click',
        ],
      },
    ],
  },
  workflow: {
    steps: [
      'Customer profile',
      'GEO diagnosis',
      'Keywords',
      'Content',
      'Landing page',
      'Leads',
      'Weekly report',
    ],
  },
  cta: {
    title: 'Ready to see your brand in AI answers?',
    body: 'Book a walkthrough with our team or sign in to your workspace.',
    bookDemo: 'Book a demo',
    login: 'Log in to Admin',
  },
  pricing: {
    metaTitle: 'Pricing — TourGEO',
    metaDescription:
      'Simple plans for China inbound tour operators: GEO diagnostics, keyword insights, AI content, and landing pages.',
    heading: 'Plans that grow with your inbound pipeline',
    subheading: 'Start with a diagnostic, scale into full content and landing delivery. No ranking guarantees.',
    tiers: [
      {
        name: 'Starter',
        price: 'Free',
        period: 'diagnostic',
        tagline: 'See where you stand in AI answers.',
        features: [
          '1 GEO diagnostic (US / UK / AU sample)',
          'Visibility score + competitor mentions',
          'Top keyword opportunities preview',
          'PDF summary report',
        ],
        cta: 'Get a free diagnostic',
      },
      {
        name: 'Growth',
        price: 'Custom',
        period: 'per month',
        tagline: 'The full inbound acquisition workflow.',
        features: [
          'Scheduled GEO diagnostics + trends',
          'Eight-stage keyword library & scoring',
          'AI content scripts with knowledge-base RAG',
          'Landing page agent + lead capture',
          'Monthly white-label reports',
        ],
        cta: 'Talk to sales',
        featured: true,
      },
      {
        name: 'Agency',
        price: 'Custom',
        period: 'multi-client',
        tagline: 'Manage many operators in one workspace.',
        features: [
          'Everything in Growth',
          'Multi-tenant client management',
          'White-label reports & portals',
          'Browser probe nodes & calibration',
          'Priority support & onboarding',
        ],
        cta: 'Talk to sales',
      },
    ],
    note: 'Prices are customized per market and volume. All AI outputs are sampled and require human review before delivery.',
    faqTitle: 'Frequently asked questions',
    faqs: [
      {
        q: 'Do you guarantee AI recommendation rankings?',
        a: 'No. We measure and improve visibility with grounded sampling, but we never promise guaranteed rankings in AI answers.',
      },
      {
        q: 'Which AI platforms do you sample?',
        a: 'Perplexity, Gemini, and OpenAI via grounded APIs, plus optional browser-extension probes for China-accessible calibration.',
      },
      {
        q: 'How is billing handled?',
        a: 'Growth and Agency plans are quota-based per month (diagnostics, keywords, content, landing pages, reports). Contact sales for a quote.',
      },
    ],
  },
  contact: {
    metaTitle: 'Contact — TourGEO',
    metaDescription: 'Book a demo or ask about GEO diagnostics for your China inbound tourism business.',
    heading: 'Talk to the TourGEO team',
    subheading: 'Tell us about your business and we will reply within 24 hours.',
    fields: {
      name: { label: 'Name', placeholder: 'Your name' },
      email: { label: 'Email', placeholder: 'you@company.com' },
      phone: { label: 'Phone / WhatsApp', placeholder: '+1 555 000 0000' },
      company: { label: 'Company', placeholder: 'Your tour operator' },
      message: { label: 'How can we help?', placeholder: 'Markets, goals, questions…' },
    },
    submit: 'Send message',
    submitting: 'Sending…',
    hint: 'We use your details only to respond to this inquiry. Email or phone is required.',
    turnstileDisabled: 'Verification disabled in local dev — submit without captcha.',
    errRequired: 'Please tell us your name.',
    errContact: 'Please provide at least an email or phone number.',
    errCaptcha: 'Please complete the verification challenge.',
    errNetwork: 'Network error. Please try again later.',
    errGeneric: 'Something went wrong. Please try again.',
    success: 'Thank you! We will reply within 24 hours.',
  },
};

const zh: Dictionary = {
  htmlLang: 'zh-CN',
  meta: {
    title: 'TourGEO 旅获 AI — 入境游海外获客增长系统',
    description:
      '面向中国入境游企业的 AI 海外获客系统：GEO 可见率诊断、海外关键词洞察、AI 内容与英文落地页。',
  },
  nav: {
    features: '功能',
    pricing: '价格',
    contact: '联系我们',
    login: '登录',
    bookDemo: '预约演示',
  },
  footer: {
    privacy: '隐私政策',
    terms: '服务条款',
    disclaimer:
      'GEO 与 AI 结果基于特定时间和平台抽样，不构成对未来可见率的保证。生成页面中的价格与签证信息需人工确认。',
    poweredBy: '由 TourGEO Agent 提供支持',
    rights: 'TourGEO / 旅获 AI',
  },
  hero: {
    subZh: '旅获 AI · 入境游海外获客增长系统',
    title: '在海外游客向 AI 提问的地方赢得他们',
    lead: 'GEO 可见率诊断、海外关键词洞察、AI 辅助的内容与英文落地页 —— 专为中国入境游企业打造。',
    compliance: '基于抽样的 AI 可见率洞察，不承诺排名保证。',
    bookDemo: '预约演示',
    login: '登录后台',
  },
  value: {
    title: '入境游企业为何选择 TourGEO',
    cards: [
      {
        icon: '📊',
        title: '看清',
        body: '在真实的海外游客提问中，看清 AI 助手提及你品牌与竞品的频率。',
      },
      {
        icon: '⚡',
        title: '行动',
        body: '锁定关键词机会，生成可复核的脚本与英文落地页文案。',
      },
      {
        icon: '📥',
        title: '转化',
        body: '发布合规落地页，带询盘表单与 WhatsApp 承接，询盘统一管理。',
      },
    ],
  },
  capabilities: {
    title: '海外获客所需的一切能力',
    cards: [
      {
        icon: '🌐',
        title: 'GEO 诊断',
        points: [
          '跨 Perplexity、Gemini、OpenAI 的联网 AI 抽样',
          '含引用覆盖的可见率评分',
          '面向美 / 英 / 澳市场的定时复测',
        ],
      },
      {
        icon: '🔑',
        title: '关键词洞察',
        points: ['用户八阶段旅程映射', '海外需求机会评分', '洞察一键导入内容工作流'],
      },
      {
        icon: '🎬',
        title: '内容 Agent',
        points: ['英文社媒脚本与分镜', '基于知识库 RAG 并标注来源', '每条生成结果均带人工复核标记'],
      },
      {
        icon: '📄',
        title: '落地页 Agent',
        points: ['对齐关键词意图的英文 SSG 页面', 'Turnstile 询盘表单与 WhatsApp 承接', '一键发布与预览'],
      },
    ],
  },
  workflow: {
    steps: ['客户资料', 'GEO 诊断', '关键词', '内容', '落地页', '询盘', '周报'],
  },
  cta: {
    title: '想看到你的品牌出现在 AI 回答里？',
    body: '预约一次演练，或直接登录你的工作台。',
    bookDemo: '预约演示',
    login: '登录后台',
  },
  pricing: {
    metaTitle: '价格 — TourGEO 旅获 AI',
    metaDescription: '面向中国入境游企业的简洁套餐：GEO 诊断、关键词洞察、AI 内容与落地页。',
    heading: '随入境游管道一起成长的套餐',
    subheading: '从一次诊断开始，扩展到完整的内容与落地页交付。不承诺排名保证。',
    tiers: [
      {
        name: '体验版',
        price: '免费',
        period: '单次诊断',
        tagline: '先看清你在 AI 回答中的位置。',
        features: ['1 次 GEO 诊断（美 / 英 / 澳抽样）', '可见率评分 + 竞品提及', '关键词机会预览', 'PDF 摘要报告'],
        cta: '免费获取诊断',
      },
      {
        name: '成长版',
        price: '定制',
        period: '每月',
        tagline: '完整的海外获客工作流。',
        features: [
          '定时 GEO 诊断 + 趋势',
          '八阶段关键词库与评分',
          '基于知识库 RAG 的 AI 内容脚本',
          '落地页 Agent + 询盘承接',
          '月度白标报告',
        ],
        cta: '联系销售',
        featured: true,
      },
      {
        name: '代理版',
        price: '定制',
        period: '多客户',
        tagline: '在一个工作台管理多家运营商。',
        features: ['包含成长版全部能力', '多租户客户管理', '白标报告与门户', '浏览器探针节点与校准', '优先支持与陪跑'],
        cta: '联系销售',
      },
    ],
    note: '价格按市场与用量定制。所有 AI 结果均为抽样，交付前需人工复核。',
    faqTitle: '常见问题',
    faqs: [
      {
        q: '你们保证 AI 推荐排名吗？',
        a: '不保证。我们用联网抽样来衡量并提升可见率，但绝不承诺在 AI 回答中的排名保证。',
      },
      {
        q: '你们抽样哪些 AI 平台？',
        a: '通过联网 API 抽样 Perplexity、Gemini 与 OpenAI，并可选浏览器扩展探针用于国内可达性校准。',
      },
      {
        q: '如何计费？',
        a: '成长版与代理版按月配额计费（诊断、关键词、内容、落地页、报告）。具体报价请联系销售。',
      },
    ],
  },
  contact: {
    metaTitle: '联系我们 — TourGEO 旅获 AI',
    metaDescription: '预约演示，或咨询面向你的中国入境游业务的 GEO 诊断。',
    heading: '联系 TourGEO 团队',
    subheading: '告诉我们你的业务情况，我们会在 24 小时内回复。',
    fields: {
      name: { label: '姓名', placeholder: '您的姓名' },
      email: { label: '邮箱', placeholder: 'you@company.com' },
      phone: { label: '电话 / WhatsApp', placeholder: '+86 138 0000 0000' },
      company: { label: '公司', placeholder: '您的旅游企业' },
      message: { label: '需要什么帮助？', placeholder: '目标市场、诉求、问题…' },
    },
    submit: '发送',
    submitting: '发送中…',
    hint: '您的信息仅用于回复本次咨询。邮箱与电话至少填写一项。',
    turnstileDisabled: '本地开发已关闭验证 —— 可直接提交。',
    errRequired: '请填写您的姓名。',
    errContact: '请至少填写邮箱或电话。',
    errCaptcha: '请完成人机验证。',
    errNetwork: '网络异常，请稍后再试。',
    errGeneric: '出错了，请重试。',
    success: '感谢！我们会在 24 小时内回复。',
  },
};

const DICTS: Record<Lang, Dictionary> = { en, zh };

export function getDict(lang: Lang): Dictionary {
  return DICTS[lang] ?? en;
}

/** Build a localized path: en → "/path", zh → "/zh/path". */
export function localizePath(lang: Lang, path: string): string {
  const clean = `/${path}`.replace(/\/+/g, '/').replace(/\/$/, '') || '/';
  if (lang === 'en') return clean === '/' ? '/' : clean;
  return clean === '/' ? '/zh' : `/zh${clean}`;
}

/** Strip the /zh prefix to get the equivalent path in the other language. */
export function switchLangPath(current: Lang, target: Lang, pathname: string): string {
  let base = pathname.replace(/\/+$/, '') || '/';
  if (current === 'zh') {
    base = base.replace(/^\/zh(\/|$)/, '/');
  }
  base = base.replace(/\/+$/, '') || '/';
  return localizePath(target, base);
}
