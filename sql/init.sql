-- ================================================
-- 纸域空间平台数据库初始化脚本
-- ================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS zhiguang DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE zhiguang;

-- ================================================
-- 用户表
-- ================================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    avatar VARCHAR(255) DEFAULT 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png' COMMENT '头像URL',
    bio VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER-普通用户 ADMIN-管理员',
    admin_position VARCHAR(20) DEFAULT NULL COMMENT '后台职位：SUPER/CONTENT/COMMUNITY/USER/SYSTEM/DATA',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常 BANNED-已封禁',
    security_version INT NOT NULL DEFAULT 0 COMMENT '认证安全版本，用于吊销旧Token',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ================================================
-- 文章表
-- ================================================
DROP TABLE IF EXISTS articles;
CREATE TABLE articles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    content TEXT COMMENT '文章内容',
    summary VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    cover VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
    user_id BIGINT NOT NULL COMMENT '作者ID',
    status INT DEFAULT 1 COMMENT '状态：1-草稿 2-已发布 3-待审核 4-已下架',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    favorite_count INT DEFAULT 0 COMMENT '收藏数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- ================================================
-- 文章标签表
-- ================================================
DROP TABLE IF EXISTS article_tags;
CREATE TABLE article_tags (
    article_id BIGINT NOT NULL,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (article_id, tag),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签表';


-- ================================================
-- 评论表
-- ================================================
DROP TABLE IF EXISTS comments;
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL COMMENT '评论内容',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID（回复）',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常 PENDING-待审核 DELETED-已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';


-- ================================================
-- 测试用户数据（密码都是 123456，BCrypt加密后）
-- ================================================
INSERT INTO users (username, email, password, avatar, bio) VALUES
('技术小能手', 'tech@example.com', '$2a$10$p1CLc10fKaBYcgTgftPjJOHe5hxgkYqXoQyeI8eMBIAN9rO012O6O', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', '热爱技术，专注于前端开发，分享Vue和React相关经验'),
('Java架构师', 'java@example.com', '$2a$10$p1CLc10fKaBYcgTgftPjJOHe5hxgkYqXoQyeI8eMBIAN9rO012O6O', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', '十年Java开发经验，擅长微服务架构设计'),
('前端架构狮', 'fe@example.com', '$2a$10$p1CLc10fKaBYcgTgftPjJOHe5hxgkYqXoQyeI8eMBIAN9rO012O6O', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', '追求极致的前端性能优化，关注用户体验'),
('Python爱好者', 'python@example.com', '$2a$10$p1CLc10fKaBYcgTgftPjJOHe5hxgkYqXoQyeI8eMBIAN9rO012O6O', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', '全栈工程师，主攻Python和数据分析'),
('全栈开发者', 'fullstack@example.com', '$2a$10$p1CLc10fKaBYcgTgftPjJOHe5hxgkYqXoQyeI8eMBIAN9rO012O6O', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', '相信技术改变世界，正在学习AI大模型');

-- ================================================
-- 预置专用管理员账号（不再将首个注册用户自动设为管理员）
-- 账号: admin@zhiguang.com  密码: Admin@2024
-- 生产环境部署后请立即修改密码
-- ================================================
INSERT INTO users (username, email, password, role, status, avatar, bio) VALUES
('admin', 'admin@zhiguang.com', '$2a$10$K4s8pA0ySzvMqw2CpDBcueW3GwAvzvKQEGvyGyhgoR6HarDUrVhHi', 'ADMIN', 'NORMAL', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', '系统管理员');

-- ================================================
-- 测试文章数据
-- ================================================
INSERT INTO articles (title, content, summary, user_id, status, view_count, like_count, favorite_count, comment_count, created_at) VALUES
('Vue 3 Composition API 全面解析', '<p>Vue 3 引入了 Composition API，这是一种全新的逻辑组织方式。本文深入探讨了 Vue 3 中的 Composition API，包括 setup、reactive、computed 等核心概念及其最佳实践。</p><h2>什么是 Composition API？</h2><p>Composition API 是 Vue 3 引入的一套新的 API 组合方式，它允许我们用函数的形式来组织组件逻辑。与传统的 Options API 相比，Composition API 提供了更灵活的方式来共享和复用逻辑代码。</p><p>在传统的 Options API 中，我们需要按照 data、methods、computed 等选项来组织代码。当组件变得复杂时，相关逻辑可能会散落在不同的选项中，导致代码难以维护和理解。而 Composition API 允许我们按照功能来组织代码，将相关的响应式数据和相关的方法放在一起。</p><h2>核心概念详解</h2><h3>1. setup() 函数</h3><p>setup() 是 Composition API 的入口点，在组件实例创建之前执行。这意味着在 setup 中无法访问 this，因为组件实例还没有被创建。setup 函数接收两个参数：props 和 context。</p><h3>2. reactive() 和 ref()</h3><p>reactive() 用于创建响应式对象，适用于复杂类型如数组、对象。ref() 用于创建响应式基本类型值，但返回的是一个对象，需要通过 .value 访问。</p><h3>3. computed() 计算属性</h3><p>computed() 用于创建计算属性，它会缓存计算结果，只有当依赖的响应式数据变化时才会重新计算。</p><h3>4. watch() 监听器</h3><p>watch() 用于监听响应式数据的变化，可以深度监听对象的变化，也可以监听多个数据源。</p><h2>生命周期钩子在 Composition API 中的使用</h2><p>在 Composition API 中，生命周期钩子需要显式导入和调用。常用的钩子包括：onMounted、onUpdated、onUnmounted、onBeforeMount、onBeforeUpdate、onBeforeUnmount 等。</p><h2>最佳实践</h2><p>1. 按功能组织代码：将相关的逻辑组织在一起，而不是按照选项类型分散放置。</p><p>2. 使用 Composables 复用逻辑：将可复用的逻辑封装到独立的函数中，这些函数就是 composables。</p><p>3. 合理使用生命周期钩子：在 setup 中使用生命周期钩子，确保逻辑与组件实例同步。</p><p>4. 注意响应式丢失问题：从 reactive 对象中解构出的数据会丢失响应性，需要使用 toRefs 保持响应性。</p><h2>总结</h2><p>Vue 3 的 Composition API 为我们提供了一种更灵活、更强大的方式来组织组件逻辑。通过合理使用 setup、reactive、ref、computed、watch 等核心概念，我们可以编写出更加清晰、可维护的代码。同时，通过 composables 的模式，我们可以轻松地复用逻辑代码，构建出更加模块化的应用程序。</p>', '深入探讨 Vue 3 中的 Composition API，包括 setup、reactive、computed 等核心概念及其最佳实践', 1, 2, 1234, 89, 45, 12, NOW() - INTERVAL 1 DAY),

('Spring Boot 3.0 核心新特性深度剖析', '<p>Spring Boot 3.0 带来了众多重磅更新，本文带你深入了解这些新特性及其在生产环境中的应用。Spring Boot 3.0 是迄今为止最具变革性的版本，引入了许多令人兴奋的新特性和改进。</p><h2>Java 17 最低要求</h2><p>Spring Boot 3.0 要求 Java 17 作为最低版本，这意味着我们可以使用密封类、模式匹配、记录类等 Java 17 新特性。这些新特性不仅让代码更加简洁，还能提供更好的类型安全性。</p><p>密封类允许我们定义受限的类层次结构，强制所有可能的子类必须在这个类中声明，从而实现更严格的类型控制。记录类则是不可变数据对象的简洁表示方式，特别适合用于数据传输对象（DTO）。</p><h2>AOT 编译与 GraalVM 原生镜像</h2><p>Spring Boot 3.0 引入了 GraalVM 原生镜像支持，这是一项革命性的改进。传统的 JVM 应用需要较长的启动时间，而 GraalVM 原生镜像可以将 Spring Boot 应用的启动时间降低到毫秒级别，同时显著减少内存占用。</p><p>原生镜像的主要优势包括：即时启动、超低内存消耗、更安全的容器化部署。但需要注意，由于是提前编译（Ahead-of-Time），一些动态特性如反射、动态类加载需要额外的配置。</p><h2>新模块 Spring Modulith</h2><p>Spring Modulith 是一个新的模块，旨在帮助开发者更好地构建模块化应用。它提供了一种结构化的方式来组织大型应用程序的代码，使得代码库更加易于理解和维护。</p><p>通过 Modulith，我们可以清晰地定义应用模块之间的边界和依赖关系，同时保持模块内部的高内聚性。这对于大型团队协作开发微服务或单体应用都非常有帮助。</p><h2>观察性增强</h2><p>Actuator 端点得到了显著增强，提供了更好的可观察性支持。新的 Observability API 与 Micrometer 集成，提供了更细粒度的指标收集和追踪能力。</p><p>新的特性包括：统一的指标 API、改进的健康指示器、自定义观测能力等。这些改进使得监控生产环境中的 Spring Boot 应用变得更加简单和全面。</p><h2>Jakarta EE 10 支持</h2><p>Spring Boot 3.0 全面迁移到了 Jakarta EE 10，这意味着所有的 javax 命名空间都已迁移到 jakarta。对于需要升级的项目，需要注意这一重大变化可能带来的影响。</p><p>常用的迁移包括：jakarta.servlet 替代 javax.servlet、jakarta.persistence 替代 javax.persistence 等。Spring Boot 提供了迁移工具来帮助开发者顺利完成这一过渡。</p><h2>性能改进</h2><p>除了新特性，Spring Boot 3.0 还在性能方面做了大量优化。核心框架的启动速度提升了约 20%，而 AOT 编译的原生镜像启动时间更是降低到了原来的十分之一。</p><p>内存占用也得到了优化，特别是在 WebFlux 反应式编程模型中，内存使用效率有了显著提升。这对于在容器环境中部署大量应用实例的场景尤为重要。</p><h2>升级建议</h2><p>对于正在使用 Spring Boot 2.x 的项目，建议制定详细的升级计划：首先确保项目使用的是 Java 17，然后逐步迁移依赖项，最后进行全面测试。建议先在非生产环境中进行充分的测试，确保所有依赖都兼容 Spring Boot 3.0。</p>', 'Spring Boot 3.0 带来了众多重磅更新，包括 Java 17 最低要求、AOT 编译支持等', 2, 2, 2341, 156, 78, 23, NOW() - INTERVAL 2 DAY),

('从零实现一个响应式前端框架', '<p>通过实现一个简易的响应式框架，深入理解响应式编程的核心原理。本文将带领大家从零开始，手把手实现一个简化版的响应式框架，类似于 Vue 的响应式系统。这个过程将帮助你深入理解 Vue 3 的响应式原理。</p><h2>响应式原理概述</h2><p>响应式编程的核心是依赖追踪和批量更新。当数据变化时，自动更新所有依赖的视图。传统的命令式编程需要手动更新 DOM，而响应式编程让我们只需关注数据的变化，框架会自动处理视图更新。</p><p>现代前端框架如 Vue、React 都采用了响应式编程范式。Vue 3 使用 Proxy 实现响应式，而 Vue 2 使用 Object.defineProperty。React 则采用了不同的方式，通过虚拟 DOM 和 reconcile 过程实现高效的视图更新。</p><h2>实现简单的 reactive</h2><p>响应式系统的核心是追踪对象属性的读取和写入。我们使用 JavaScript 的 Proxy 来拦截对象的操作。</p><p>get 拦截器用于收集依赖，当属性被读取时，我们将当前的 effect 函数添加到依赖列表中。set 拦截器用于触发更新，当属性被修改时，我们通知所有依赖的 effect 函数重新执行。</p><h2>实现依赖收集系统</h2><p>依赖收集是响应式系统的核心。每个响应式对象的属性都可能依赖于一些副作用函数（effect）。当这些属性变化时，需要通知所有依赖的副作用函数重新执行。</p><p>我们可以使用一个 WeakMap 来存储 target 到 depsMap 的映射，depsMap 存储属性到 effect 列表的映射。当触发更新时，遍历所有依赖的 effect 并执行。</p><h2>实现 computed</h2><p>computed 本质上是一个特殊的 effect，它会缓存计算结果。只有当依赖的响应式数据变化时，才会重新计算。</p><p>我们可以使用一个 dirty 标志来追踪计算结果是否过期。当值被读取时，如果 dirty 为 true，则重新计算并缓存结果。</p><h2>实现 watch</h2><p>watch 用于监听数据变化，执行回调函数。它本质上也是一个 effect，但提供了更灵活的 API 来处理变化前后的值。</p><p>watch 可以监听一个或多个响应式数据源，当数据变化时执行回调函数。回调函数接收新值和旧值作为参数。</p><h2>完整的响应式系统</h2><p>将以上各个部分组合起来，我们就实现了一个简易但完整的响应式系统。这个系统支持：reactive、ref、computed、watch、effect 等核心功能。</p><p>在实际的响应式框架中，还需要考虑更多优化：异步批量更新、计算属性的懒计算、watchEffect 的立即执行、停止 effect 等。这些优化使得框架在处理大量响应式数据时依然保持高性能。</p><h2>总结</h2><p>通过从零实现响应式系统，我们深入理解了依赖追踪、批量更新、Proxy 拦截等核心概念。这些知识不仅帮助我们更好地使用 Vue，也为我们自定义前端框架打下了基础。响应式编程是现代前端开发的重要范式，值得每一个前端工程师深入学习。</p>', '通过实现一个简易的响应式框架，深入理解响应式编程的核心原理', 3, 2, 856, 67, 32, 8, NOW() - INTERVAL 3 DAY),

('TypeScript 类型体操入门', '<p>TypeScript 的类型系统非常强大，本文教你如何玩转类型体操。类型体操是 TypeScript 中一个有趣且强大的领域，它允许我们在类型层面进行复杂的计算和逻辑推理。</p><h2>基础类型回顾</h2><p>TypeScript 的基础类型包括 string、number、boolean、null、undefined、symbol、bigint 以及 void、never、object 等。了解这些基础类型是进行类型体操的前提。</p><p>除了基础类型，TypeScript 还支持字面量类型、联合类型、交叉类型等高级类型。类型别名、接口、枚举等机制让我们可以定义复杂的类型结构。</p><h2>泛型基础</h2><p>泛型让我们可以编写可重用的组件，支持多种类型。它就像是类型的变量，在使用时才确定具体的类型。</p><p>泛型可以用于函数、接口、类、类型别名等场景。合理使用泛型可以让代码更加灵活，同时保持类型安全。</p><h2>条件类型</h2><p>条件类型是一种根据其他类型推导类型的强大工具。它的语法类似于三元表达式：T extends U ? X : Y。</p><p>条件类型与映射类型结合，可以实现非常强大的类型转换功能。TypeScript 内置的工具类型如 Partial、Required、Pick、Omit 等都是基于条件类型和映射类型实现的。</p><h2>映射类型</h2><p>映射类型允许我们从现有类型创建新类型，通过遍历一个类型的所有属性并对其进行某种转换。</p><p>使用 as 子句，我们甚至可以在映射类型中重命名属性。这是 TypeScript 4.1 引入的新特性。</p><h2>模板字面量类型</h2><p>TypeScript 4.1 引入了模板字面量类型，允许我们通过模板字符串创建新的类型。</p><h2>递归类型</h2><p>TypeScript 支持递归类型定义，这在处理嵌套数据结构时非常有用，比如处理 JSON 结构。</p><h2>内置工具类型详解</h2><p>TypeScript 提供了一系列内置工具类型，它们都是基于条件类型和映射类型实现的：</p><ul><li>Partial T：将所有属性变为可选</li><li>Required T：将所有属性变为必填</li><li>Readonly T：将所有属性变为只读</li><li>Pick T, K：从 T 中选取指定的属性 K</li><li>Omit T, K：从 T 中移除指定的属性 K</li><li>Exclude T, U：从 T 中排除可以赋值给 U 的类型</li><li>Extract T, U：从 T 中提取可以赋值给 U 的类型</li><li>NonNullable T：从 T 中排除 null 和 undefined</li><li>ReturnType T：获取函数返回类型</li><li>InstanceType T：获取构造函数实例类型</li></ul><h2>实际应用案例</h2><p>类型体操在实际开发中有很多应用场景：API 响应类型处理、表单验证类型定义、路由参数类型化、状态管理类型约束等。</p><h2>总结</h2><p>TypeScript 类型体操是一个深奥但非常有价值的领域。掌握类型体操不仅能写出更健壮的代码，还能提升对 TypeScript 整体的理解。建议从基础做起，逐步深入，在实际项目中不断练习和应用。</p>', 'TypeScript 类型系统非常强大，学会类型体操可以写出更健壮的代码', 1, 2, 1589, 112, 56, 15, NOW() - INTERVAL 4 DAY),

('Python异步编程实战', '<p>异步编程是现代Python开发的重要技能。本文通过实战案例讲解 asyncio 的使用，帮助你掌握Python异步编程的核心技能。异步编程能够显著提高 I/O 密集型应用的性能。</p><h2>为什么需要异步编程</h2><p>在传统的同步编程模型中，当程序执行 I/O 操作（如网络请求、文件读写、数据库查询）时，线程会被阻塞，等待 I/O 操作完成。在这段时间内，线程无法执行其他任务，这就造成了资源的浪费。</p><p>异步编程通过事件循环和协程，在等待 I/O 操作时可以切换到其他任务，从而提高程序的并发性和吞吐量。特别是在处理大量网络请求时，异步编程能够显著提升性能。</p><h2>async/await 基础</h2><p>Python 3.5 引入了 async/await 语法，这是编写异步代码的基础。async def 用于定义一个协程函数，而 await 用于等待另一个协程完成。</p><p>协程函数与普通函数不同，调用协程函数不会立即执行，而是返回一个协程对象。需要通过 asyncio.run() 或 await 来运行它。</p><h2>并发执行多个任务</h2><p>asyncio.gather() 允许我们并发执行多个协程，并收集它们的结果。这比串行执行要快得多。</p><h2>异步上下文管理器</h2><p>异步上下文管理器使用 async with 语法，用于处理异步资源的获取和释放。常见的应用场景包括异步数据库连接、异步文件操作等。</p><h2>异步迭代器</h2><p>异步迭代器允许我们在迭代过程中执行异步操作。这在处理流式数据时非常有用，比如逐行读取大文件或流式 API 响应。</p><h2>实战：异步HTTP请求</h2><p>使用 aiohttp 库进行异步 HTTP 请求是 Python 异步编程最常见的应用场景之一。相比 requests 库的同步请求，aiohttp 能够显著提升批量请求的速度。</p><h2>异步数据库操作</h2><p>对于数据库操作，可以使用 aiomysql、asyncpg、aiosqlite 等异步数据库驱动。这些库提供了与对应同步库相似的 API，但使用异步方式执行操作。</p><h2>错误处理和重试机制</h2><p>在实际应用中，异步操作也可能失败，需要优雅地处理错误并实现重试机制。</p><h2>异步定时任务</h2><p>asyncio 也支持定时任务，虽然不如专门的调度库如 Celery 强大，但对于简单的定时任务已经足够。</p><h2>性能对比</h2><p>在网络 I/O 密集型任务中，异步版本通常比同步版本快 5-10 倍甚至更多。</p><h2>总结</h2><p>Python 异步编程是一个强大的技能，掌握 asyncio 和 async/await 语法能够让你编写出高效的网络应用和爬虫程序。关键是要理解协程、事件循环、任务调度等核心概念，并在实际项目中不断练习和应用。对于 I/O 密集型应用，异步编程是提升性能的利器。</p>', '通过实战案例讲解 asyncio 的使用，提升 Python 并发编程能力', 4, 2, 967, 78, 41, 9, NOW() - INTERVAL 5 DAY),

('前端性能优化完全指南', '<p>性能是用户体验的关键。本文汇总了前端性能优化的各种技巧，从加载性能到渲染性能的完整指南，帮助你打造高性能的 Web 应用。用户对页面加载时间的忍耐度只有几秒钟，如果你的网站加载过慢，用户会毫不犹豫地离开。</p><h2>性能指标与度量</h2><p>在开始优化之前，我们需要了解关键的网页性能指标。Core Web Vitals 是 Google 提出的核心性能指标，包括：</p><ul><li>LCP (Largest Contentful Paint)：最大内容绘制时间，衡量加载性能。好的 LCP 在 2.5 秒以内。</li><li>FID (First Input Delay)：首次输入延迟，衡量交互性。好的 FID 在 100 毫秒以内。</li><li>CLS (Cumulative Layout Shift)：累计布局偏移，衡量视觉稳定性。好的 CLS 在 0.1 以内。</li></ul><p>使用 Chrome DevTools、Lighthouse、WebPageTest 等工具可以测量这些指标。</p><h2>加载性能优化</h2><h3>1. 代码分割与懒加载</h3><p>代码分割是减少初始加载体积的关键技术。通过将代码分割成多个小块，可以实现按需加载，只下载当前页面需要的代码。</p><h3>2. Tree Shaking</h3><p>Tree Shaking 是一个消除无用代码的技术。在打包时，构建工具会自动移除未被使用的代码。这要求我们使用 ES6 模块语法，因为 CommonJS 模块无法被 Tree Shaking。</p><h3>3. 资源压缩与优化</h3><p>压缩资源可以显著减少传输体积。常见的压缩方式包括：Gzip/Brotli 压缩、图片优化、CSS/JS 压缩等。</p><h3>4. 预加载与预获取</h3><p>使用 link 标签的 rel 属性可以预加载关键资源或预获取将来可能需要的资源。</p><h3>5. HTTP 缓存策略</h3><p>合理设置缓存策略可以避免重复下载资源。使用 Cache-Control、ETag、Last-Modified 等 HTTP 头可以控制浏览器的缓存行为。</p><h2>渲染性能优化</h2><h3>1. 避免重排和重绘</h3><p>重排（reflow）和重绘（repaint）是影响渲染性能的主要原因。当 DOM 元素的几何属性发生变化时会发生重排，当外观改变但不影响布局时会发生重绘。</p><h3>2. 使用 CSS contain 属性</h3><p>CSS contain 属性允许我们声明元素的独立性，帮助浏览器优化渲染性能。</p><h3>3. will-change 优化</h3><p>will-change 属性告诉浏览器元素即将发生哪些变化，让浏览器提前做优化准备。但不要滥用，否则会适得其反。</p><h3>4. 使用 transform 和 opacity</h3><p>transform 和 opacity 属性的变化不会触发重排，只触发重绘，而且是 GPU 加速的，性能最好。</p><h2>运行时性能优化</h2><h3>1. 防抖和节流</h3><p>防抖和节流是控制函数执行频率的技术，对于处理频繁触发的事件（如 scroll、resize、input）非常有用。</p><h3>2. 虚拟列表</h3><p>对于长列表，只渲染可见区域的元素可以大大提升性能。虚拟列表是处理大量数据的利器。</p><h3>3. Web Worker</h3><p>Web Worker 允许我们在后台线程执行 JavaScript，避免阻塞主线程。对于复杂的计算任务特别有用。</p><h2>内存管理与泄漏检测</h2><h3>1. 常见内存泄漏场景</h3><p>常见内存泄漏场景包括：全局变量未清理、定时器未清除、事件监听器未移除、闭包引用、DOM 引用等。</p><h2>总结</h2><p>前端性能优化是一个系统工程，需要从加载、渲染、运行时等多个维度综合考虑。通过合理的优化策略，我们可以显著提升用户体验，让应用更快、更流畅。记住，优化要有的放矢，用性能工具先定位问题，再针对性地优化。</p>', '前端性能优化全攻略，从加载性能到渲染性能的完整指南', 3, 2, 2156, 189, 92, 28, NOW() - INTERVAL 6 DAY),

('Docker容器化部署实战', '<p>本文介绍如何使用 Docker 部署 Spring Boot 应用。从 Dockerfile 编写到 CI/CD 集成，详细介绍 Docker 容器化部署 Spring Boot 应用的最佳实践。容器化已经成为现代应用部署的标准方式。</p><h2>Docker 简介</h2><p>Docker 是一个开源的容器化平台，允许开发者将应用及其依赖打包到一个轻量级、可移植的容器中。容器与虚拟机类似，但更加轻量，因为容器共享宿主机的操作系统内核。</p><p>Docker 的主要优势包括：一致性环境、快速启动、资源高效利用、易于扩展和迁移。</p><h2>Dockerfile 编写</h2><p>Dockerfile 是用于构建 Docker 镜像的脚本文件。一个典型的 Spring Boot 应用的 Dockerfile 包含基础镜像、工作目录、依赖安装、构建步骤和启动命令等部分。</p><h2>多阶段构建</h2><p>多阶段构建可以显著减小最终镜像的体积。上面的示例使用了 Maven 构建阶段和精简 JRE 运行阶段，最终镜像只包含运行时需要的内容。</p><p>多阶段构建的好处：构建阶段可以使用完整的 JDK 和 Maven，运行阶段只使用精简的 JRE 或甚至是无 JDK 的镜像，最终镜像体积可以减少 50-80%。</p><h2>docker-compose 编排</h2><p>docker-compose 允许我们定义和运行多容器应用。对于包含数据库、Redis 等依赖的应用，使用 docker-compose 可以方便地管理所有容器。</p><h2>应用配置</h2><p>在容器化环境中，应用的配置应该通过环境变量传递，而不是硬编码在配置文件中。Spring Boot 应用读取环境变量非常方便。</p><h2>使用 Docker Secret 管理敏感信息</h2><p>在生产环境中，不应该将密码等敏感信息明文写在配置或环境变量中。Docker Swarm 提供了 Secret 功能来安全地管理敏感数据。</p><h2>健康检查配置</h2><p>配置健康检查可以确保容器在应用启动完成后才开始接收流量，Docker 会定期检查容器健康状态。</p><h2>日志管理</h2><p>容器中的日志应该输出到标准输出和标准错误，而不是写入文件。使用 Docker 的日志驱动可以方便地收集和管理日志。</p><h2>资源限制</h2><p>为容器设置资源限制可以防止某个应用耗尽宿主机的资源，影响其他应用。</p><h2>CI/CD 集成</h2><p>将 Docker 集成到 CI/CD 流程中，可以实现自动化构建、测试和部署。</p><h2>使用 Kubernetes 部署</h2><p>对于大规模应用，可以使用 Kubernetes 进行容器编排。Docker Compose 适合开发和测试环境，而 Kubernetes 是生产环境的首选。</p><h2>镜像安全最佳实践</h2><ul><li>使用官方基础镜像并定期更新</li><li>不要在镜像中存储凭据，使用环境变量或 Docker Secret</li><li>以非 root 用户运行容器</li><li>定期扫描镜像漏洞</li><li>使用可信的镜像仓库</li></ul><h2>总结</h2><p>Docker 容器化为 Spring Boot 应用部署提供了标准化、可重复的部署方式。通过 Dockerfile、docker-compose、CI/CD 集成等工具和最佳实践，我们可以构建高效的容器化部署流程。结合 Kubernetes，可以实现大规模生产环境的高可用和自动扩缩容。</p>', '详细介绍 Docker 容器化部署 Spring Boot 应用的最佳实践', 2, 2, 1456, 98, 67, 18, NOW() - INTERVAL 7 DAY),

('React 18 新特性抢先看', '<p>React 18 带来了哪些新特性？本文为你一一解析，包括 Concurrent Mode、自动批处理等重磅更新。React 18 是近年来 React 最重要的版本升级，引入了全新的并发渲染引擎。</p><h2>React 18 核心改进概述</h2><p>React 18 的核心改进包括：全新的并发渲染器、Automatic Batching 自动批处理、新的 Suspense 架构、新的 Hooks API 以及服务器组件支持。这些改进让 React 应用可以更好地利用用户的设备能力，提供更流畅的用户体验。</p><p>React 18 的设计理念是：让 React 应用可以同时准备多个版本的 UI。这意味着 React 可以在后台准备新状态的渲染，同时保持当前界面的响应性。</p><h2>Concurrent Mode 并发模式</h2><p>并发模式是 React 18 最重要的特性。它不是某个具体的 API，而是一种全新的渲染策略。并发模式允许 React 中断、暂停、恢复和放弃渲染工作。</p><p>并发模式的主要好处：可中断渲染、选择性水合、Streaming SSR with Suspense 等。</p><h2>Automatic Batching 自动批处理</h2><p>自动批处理是 React 18 带来的重大改进。在 React 17 及之前，只有 React 事件处理函数中的多次 setState 会被批处理，而 Promise、setTimeout、原生事件等中的 setState 不会自动批处理。</p><p>React 18 自动批处理所有场景，无论在哪里触发更新都会被批处理。</p><h2>新的 Suspense 架构</h2><p>Suspense 组件用于在数据加载过程中显示后备内容。React 18 中的 Suspense 经过了重新设计，支持服务端渲染和更好的用户体验。</p><p>新的 Suspense 特性包括：Streaming SSR 和 Selective Hydration。</p><h2>新的 Hooks</h2><h3>useTransition</h3><p>useTransition 用于标记非紧急更新，允许 React 在紧急更新（如输入、点击）时中断非紧急更新。</p><h3>useDeferredValue</h3><p>useDeferredValue 用于延迟更新非紧急部分，与 useTransition 类似，但更适用于值而非更新函数。</p><h3>useSyncExternalStore</h3><p>useSyncExternalStore 是一个新的 Hook，用于在组件中订阅外部数据源。它提供了同步读取和强制更新机制，确保在并发模式下数据一致性。</p><h3>useInsertionEffect</h3><p>useInsertionEffect 是 useLayoutEffect 的新版本，专门用于 CSS-in-JS 库在 DOM 变化前注入样式。</p><h2>服务端组件 (React Server Components)</h2><p>React 服务端组件是 React 18 的另一个重要特性，但需要额外的服务端支持。它允许组件只在服务端渲染，减少客户端 bundle 大小。</p><h2>迁移到 React 18</h2><p>React 18 设计为向后兼容，大多数现有代码无需修改即可在 React 18 中工作。但需要注意以下几点：新的 Root API、Strict Mode 行为变化、依赖于渲染顺序的代码等。</p><h2>总结</h2><p>React 18 为我们带来了并发渲染、自动批处理、新的 Suspense 架构和一系列新 Hooks。这些改进让 React 应用可以提供更流畅的用户体验，特别是对于复杂应用尤为重要。建议开发者逐步学习和尝试这些新特性，并计划好项目的升级路径。</p>', 'React 18 新特性解析，包括 Concurrent Mode、自动批处理等重磅更新', 1, 2, 1876, 145, 73, 21, NOW() - INTERVAL 8 DAY);

-- ================================================
-- 文章标签数据
-- ================================================
INSERT INTO article_tags (article_id, tag) VALUES
(1, '前端'), (1, 'Vue'), (1, 'JavaScript'),
(2, '后端'), (2, 'Java'), (2, 'Spring Boot'),
(3, '前端'), (3, '架构'), (3, 'JavaScript'),
(4, '前端'), (4, 'TypeScript'),
(5, '后端'), (5, 'Python'), (5, '异步编程'),
(6, '前端'), (6, '性能优化'),
(7, 'DevOps'), (7, 'Docker'),
(8, '前端'), (8, 'React');

-- ================================================
-- 评论数据
-- ================================================
INSERT INTO comments (content, article_id, user_id, parent_id, created_at) VALUES
('写得很好，对我帮助很大！期待更多 Vue 3 的文章！', 1, 2, NULL, NOW() - INTERVAL 1 DAY),
('感谢分享！Composition API 确实很好用', 1, 3, NULL, NOW() - INTERVAL 1 DAY),
('写得不错，但是感觉还可以更深入一些', 1, 4, 1, NOW() - INTERVAL 12 HOUR),
('同意楼上的看法，不过对于入门来说已经足够了', 1, 5, 3, NOW() - INTERVAL 6 HOUR),
('Spring Boot 3.0 的 AOT 编译支持很强大！', 2, 1, NULL, NOW() - INTERVAL 2 DAY),
('Java 17 的新特性终于可以用了，开心！', 2, 3, NULL, NOW() - INTERVAL 2 DAY),
('这个实现思路很清晰，对理解原理很有帮助', 3, 1, NULL, NOW() - INTERVAL 3 DAY),
('TypeScript 类型体操确实很有意思', 4, 2, NULL, NOW() - INTERVAL 4 DAY),
('asyncio 确实是 Python 异步编程的核心', 5, 1, NULL, NOW() - INTERVAL 5 DAY),
('性能优化是前端永恒的话题', 6, 4, NULL, NOW() - INTERVAL 6 DAY),
('Docker 部署确实方便了很多', 7, 5, NULL, NOW() - INTERVAL 7 DAY),
('React 18 终于来了！', 8, 2, NULL, NOW() - INTERVAL 8 DAY);

-- ================================================
-- 草稿文章
-- ================================================
INSERT INTO articles (title, content, summary, user_id, status, created_at) VALUES
('我的第一篇博客', '<p>这是我正在写的第一篇博客...</p>', '记录学习历程', 1, 1, NOW()),
('2024年技术展望', '<p>新的一年，技术趋势会是怎样的呢...</p>', '技术趋势预测', 2, 1, NOW());

-- ================================================
-- 知识库分类表
-- ================================================
DROP TABLE IF EXISTS knowledge_categories;
CREATE TABLE knowledge_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID',
    icon VARCHAR(50) COMMENT '图标',
    color VARCHAR(20) COMMENT '颜色',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES knowledge_categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分类表';

-- ================================================
-- 知识库条目表（完整缓存文章内容）
-- ================================================
DROP TABLE IF EXISTS knowledge_items;
CREATE TABLE knowledge_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',

    -- 缓存的文章内容（完整独立存储）
    cached_title VARCHAR(200) NOT NULL COMMENT '缓存标题',
    cached_content LONGTEXT COMMENT '缓存内容',
    cached_summary VARCHAR(500) COMMENT '缓存摘要',
    cached_cover VARCHAR(500) COMMENT '缓存封面',
    cached_tags JSON COMMENT '缓存标签',
    cached_author_name VARCHAR(50) COMMENT '缓存作者名',
    cached_author_avatar VARCHAR(255) COMMENT '缓存作者头像',

    -- 原文章引用（可选，用于追踪）
    original_article_id BIGINT COMMENT '原文章ID',
    original_deleted BOOLEAN DEFAULT FALSE COMMENT '原文章是否已删除',

    -- 分类和标签
    category_id BIGINT COMMENT '分类ID',
    notes TEXT COMMENT '个人笔记',

    -- 阅读状态
    read_status VARCHAR(20) DEFAULT 'UNREAD' COMMENT '阅读状态：UNREAD-未读 READING-在读 READ-已读',

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES knowledge_categories(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id),
    INDEX idx_original_article_id (original_article_id),
    FULLTEXT INDEX ft_content (cached_title, cached_content, cached_summary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库条目表';

-- ================================================
-- 知识库自定义标签表
-- ================================================
DROP TABLE IF EXISTS knowledge_item_tags;
CREATE TABLE knowledge_item_tags (
    item_id BIGINT NOT NULL COMMENT '知识库条目ID',
    tag VARCHAR(50) NOT NULL COMMENT '标签名',
    PRIMARY KEY (item_id, tag),
    FOREIGN KEY (item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库自定义标签表';

-- ================================================
-- RAG问答历史表
-- ================================================
DROP TABLE IF EXISTS rag_chat_history;
CREATE TABLE rag_chat_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question TEXT NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '回答',
    sources JSON COMMENT '引用的文档ID列表',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG问答历史表';

-- ================================================
-- 性能优化索引（P0）
-- 针对高频查询字段建立索引，避免全表扫描
-- ================================================

-- 文章表索引
-- 首页文章查询联合索引（status + created_at DESC），覆盖"已发布文章按时间倒序"
-- 文章全文索引（标题+正文），支持关键词搜索

-- 评论表索引

-- ================================================
-- 通知表
-- ================================================
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收通知的用户',
    type VARCHAR(50) NOT NULL COMMENT '通知类型',
    content VARCHAR(500) NOT NULL COMMENT '通知内容',
    article_id BIGINT COMMENT '相关文章ID',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ================================================
-- 关注关系表
-- ================================================
DROP TABLE IF EXISTS user_follows;
CREATE TABLE user_follows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL COMMENT '关注者',
    following_id BIGINT NOT NULL COMMENT '被关注者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_follower_following (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

-- ================================================
-- 点赞表
-- ================================================
DROP TABLE IF EXISTS article_likes;
CREATE TABLE article_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_article_user (article_id, user_id),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- ================================================
-- 收藏表
-- ================================================
DROP TABLE IF EXISTS article_favorites;
CREATE TABLE article_favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_fav_article_user (article_id, user_id),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 通知表索引
-- 联合索引（user_id + is_read），加速未读数统计

-- 关注关系表索引

-- 点赞表索引（unique 约束已自动创建 article_id+user_id 联合索引，补充单列索引）

-- 收藏表索引

-- ================================================
-- 验证查询
-- ================================================
SELECT '用户数据' as '表名', COUNT(*) as '数量' FROM users
UNION ALL
SELECT '文章数据', COUNT(*) FROM articles
UNION ALL
SELECT '评论数据', COUNT(*) FROM comments
UNION ALL
SELECT '标签数据', COUNT(*) FROM article_tags;

-- ================================================
-- V2.0 新增表
-- ================================================

-- 专栏系统
DROP TABLE IF EXISTS column_articles;
DROP TABLE IF EXISTS column_subscriptions;
DROP TABLE IF EXISTS `columns`;
CREATE TABLE `columns` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '专栏名称',
    description TEXT COMMENT '专栏描述',
    cover VARCHAR(500) COMMENT '封面',
    user_id BIGINT NOT NULL COMMENT '创建者',
    subscriber_count INT DEFAULT 0 COMMENT '订阅数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_column_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专栏表';

CREATE TABLE column_articles (
    column_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (column_id, article_id),
    FOREIGN KEY (column_id) REFERENCES `columns`(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专栏文章关联表';

CREATE TABLE column_subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    column_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_column (user_id, column_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (column_id) REFERENCES `columns`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专栏订阅表';

-- 文章合集
DROP TABLE IF EXISTS collection_articles;
DROP TABLE IF EXISTS collections;
CREATE TABLE collections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '合集名称',
    description TEXT COMMENT '合集描述',
    user_id BIGINT NOT NULL COMMENT '创建者',
    article_count INT DEFAULT 0 COMMENT '文章数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章合集表';

CREATE TABLE collection_articles (
    collection_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (collection_id, article_id),
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合集文章关联表';

-- 文章历史版本
DROP TABLE IF EXISTS article_versions;
CREATE TABLE article_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    title VARCHAR(200) COMMENT '标题',
    content TEXT COMMENT '内容',
    version INT NOT NULL COMMENT '版本号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    INDEX idx_article_version (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章历史版本表';

-- 知识图谱
DROP TABLE IF EXISTS knowledge_edges;
DROP TABLE IF EXISTS knowledge_nodes;
CREATE TABLE knowledge_nodes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '概念名称',
    type VARCHAR(50) COMMENT '类型: concept/person/tech',
    properties JSON COMMENT '属性',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱节点表';

CREATE TABLE knowledge_edges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_id BIGINT NOT NULL COMMENT '源节点',
    target_id BIGINT NOT NULL COMMENT '目标节点',
    relation VARCHAR(100) COMMENT '关系描述',
    weight DOUBLE DEFAULT 1.0 COMMENT '权重',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_id) REFERENCES knowledge_nodes(id) ON DELETE CASCADE,
    FOREIGN KEY (target_id) REFERENCES knowledge_nodes(id) ON DELETE CASCADE,
    INDEX idx_edge_source (source_id),
    INDEX idx_edge_target (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱边表';

-- ================================================
-- 专栏测试数据
-- ================================================
INSERT INTO `columns` (name, description, cover, user_id, subscriber_count, created_at) VALUES
('Vue 3 深度学习专栏', '从入门到精通，全面掌握 Vue 3 Composition API、响应式原理、性能优化等核心知识。适合有一定 Vue 2 基础的开发者进阶学习。', 'https://picsum.photos/seed/vue3/800/400', 1, 128, NOW() - INTERVAL 10 DAY),
('Java 企业级开发专栏', '十年 Java 架构师经验分享，涵盖 Spring Boot、微服务、容器化部署等企业级开发最佳实践。', 'https://picsum.photos/seed/javaee/800/400', 2, 256, NOW() - INTERVAL 15 DAY),
('前端性能优化专栏', '追求极致的用户体验，从加载性能到渲染性能，全方位优化前端应用的性能指标。', 'https://picsum.photos/seed/feprof/800/400', 3, 89, NOW() - INTERVAL 8 DAY),
('Python 异步编程专栏', '深入理解 Python asyncio，掌握异步编程的核心技能，构建高性能的网络应用。', 'https://picsum.photos/seed/pyasync/800/400', 4, 67, NOW() - INTERVAL 12 DAY);

-- 专栏文章关联
INSERT INTO column_articles (column_id, article_id, sort_order) VALUES
-- Vue 3 深度学习专栏（专栏1）：文章1、4、8
(1, 1, 1),  -- Vue 3 Composition API 全面解析
(1, 4, 2),  -- TypeScript 类型体操入门
(1, 8, 3),  -- React 18 新特性抢先看（对比学习）

-- Java 企业级开发专栏（专栏2）：文章2、7
(2, 2, 1),  -- Spring Boot 3.0 核心新特性深度剖析
(2, 7, 2),  -- Docker容器化部署实战

-- 前端性能优化专栏（专栏3）：文章3、6
(3, 3, 1),  -- 从零实现一个响应式前端框架
(3, 6, 2),  -- 前端性能优化完全指南

-- Python 异步编程专栏（专栏4）：文章5
(4, 5, 1);  -- Python异步编程实战

-- ================================================
-- 合集测试数据
-- ================================================
INSERT INTO collections (name, description, user_id, article_count, created_at) VALUES
('前端技术精选', '精选前端技术文章，涵盖 Vue、React、TypeScript、性能优化等多个领域。', 1, 5, NOW() - INTERVAL 5 DAY),
('后端开发实战', '后端开发实战经验合集，包含 Java、Python、Docker 等技术栈的深度实践。', 2, 3, NOW() - INTERVAL 7 DAY),
('全栈技术合集', '全栈开发者必读，从前端到后端，从开发到部署的完整技术链路。', 4, 4, NOW() - INTERVAL 3 DAY),
('架构设计精选', '软件架构设计精华文章，学习如何构建可扩展、高性能的系统架构。', 3, 3, NOW() - INTERVAL 4 DAY);

-- 合集文章关联
INSERT INTO collection_articles (collection_id, article_id, sort_order) VALUES
-- 前端技术精选（合集1）：文章1、3、4、6、8
(1, 1, 1),  -- Vue 3 Composition API 全面解析
(1, 3, 2),  -- 从零实现一个响应式前端框架
(1, 4, 3),  -- TypeScript 类型体操入门
(1, 6, 4),  -- 前端性能优化完全指南
(1, 8, 5),  -- React 18 新特性抢先看

-- 后端开发实战（合集2）：文章2、5、7
(2, 2, 1),  -- Spring Boot 3.0 核心新特性深度剖析
(2, 5, 2),  -- Python异步编程实战
(2, 7, 3),  -- Docker容器化部署实战

-- 全栈技术合集（合集3）：文章1、2、5、7
(3, 1, 1),  -- Vue 3 Composition API 全面解析
(3, 2, 2),  -- Spring Boot 3.0 核心新特性深度剖析
(3, 5, 3),  -- Python异步编程实战
(3, 7, 4),  -- Docker容器化部署实战

-- 架构设计精选（合集4）：文章3、6、7
(4, 3, 1),  -- 从零实现一个响应式前端框架
(4, 6, 2),  -- 前端性能优化完全指南
(4, 7, 3);  -- Docker容器化部署实战

-- ================================================
-- 专栏订阅数据
-- ================================================
INSERT INTO column_subscriptions (user_id, column_id, created_at) VALUES
(2, 1, NOW() - INTERVAL 5 DAY),  -- Java架构师订阅了 Vue 3 专栏
(3, 1, NOW() - INTERVAL 3 DAY),  -- 前端架构狮订阅了 Vue 3 专栏
(1, 2, NOW() - INTERVAL 4 DAY),  -- 技术小能手订阅了 Java 企业级专栏
(5, 2, NOW() - INTERVAL 2 DAY),  -- 全栈开发者订阅了 Java 企业级专栏
(1, 3, NOW() - INTERVAL 1 DAY),  -- 技术小能手订阅了前端性能优化专栏
(4, 3, NOW() - INTERVAL 6 DAY);  -- Python爱好者订阅了前端性能优化专栏

-- ================================================
-- 成长体系表
-- ================================================

-- 用户等级与经验
DROP TABLE IF EXISTS experience_logs;
DROP TABLE IF EXISTS user_levels;
CREATE TABLE user_levels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    level INT DEFAULT 1 COMMENT '等级',
    experience INT DEFAULT 0 COMMENT '经验值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_level_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户等级表';

CREATE TABLE experience_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    source VARCHAR(50) NOT NULL COMMENT '经验来源',
    points INT NOT NULL COMMENT '获得经验',
    balance INT NOT NULL COMMENT '经验余额',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_experience_log_user (user_id),
    INDEX idx_experience_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='经验流水表';

-- 成就系统
DROP TABLE IF EXISTS user_achievements;
DROP TABLE IF EXISTS achievements;
CREATE TABLE achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '成就编码',
    name VARCHAR(100) NOT NULL COMMENT '成就名称',
    description TEXT COMMENT '成就描述',
    icon VARCHAR(255) COMMENT '图标',
    required_condition VARCHAR(255) COMMENT '解锁条件描述',
    reward_points INT DEFAULT 0 COMMENT '奖励积分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_achievement_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就表';

CREATE TABLE user_achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    achievement_id BIGINT NOT NULL COMMENT '成就ID',
    obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    UNIQUE KEY uk_user_achievement (user_id, achievement_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
    INDEX idx_user_achievement_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就表';

-- 积分与签到
DROP TABLE IF EXISTS point_logs;
DROP TABLE IF EXISTS user_points;
DROP TABLE IF EXISTS daily_sign_ins;
CREATE TABLE user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    points INT DEFAULT 0 COMMENT '当前积分',
    total_points INT DEFAULT 0 COMMENT '累计积分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_point_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分表';

CREATE TABLE point_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(50) NOT NULL COMMENT '积分类型',
    amount INT NOT NULL COMMENT '变动数量',
    balance INT NOT NULL COMMENT '积分余额',
    description VARCHAR(255) COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_point_log_user (user_id),
    INDEX idx_point_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水表';

CREATE TABLE daily_sign_ins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sign_date DATE NOT NULL COMMENT '签到日期',
    continuous_days INT DEFAULT 1 COMMENT '连续天数',
    reward_points INT DEFAULT 0 COMMENT '奖励积分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_sign_date (user_id, sign_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_daily_sign_in_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日签到表';

-- ================================================
-- 成就初始化数据
-- ================================================
INSERT INTO achievements (code, name, description, icon, required_condition, reward_points) VALUES
('FIRST_POST', '首次发文', '在平台发布第一篇文章', 'write', '发布1篇文章', 50),
('COMMENT_MASTER', '评论达人', '累计发表评论10次', 'comment', '累计评论10次', 30),
('COLLECTION_EXPERT', '收藏专家', '累计获得10次文章收藏', 'bookmark', '文章被收藏10次', 50),
('LIKES_100', '获得100点赞', '累计获得100个文章点赞', 'heart', '获得100个点赞', 100),
('VIEWS_1000', '获得1000阅读', '累计获得1000次文章阅读', 'eye', '获得1000次阅读', 100),
('SIGN_IN_7_DAYS', '连续签到7天', '连续签到7天不间断', 'fire', '连续签到7天', 50);

-- ================================================
-- 运营后台表
-- ================================================

-- 举报表
DROP TABLE IF EXISTS reports;
CREATE TABLE reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型：ARTICLE/COMMENT/USER',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    reason VARCHAR(500) COMMENT '举报理由',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理 HANDLED-已处理 DISMISSED-已驳回',
    handler_id BIGINT COMMENT '处理人（管理员ID）',
    handle_result TEXT COMMENT '处理结果说明',
    handled_at TIMESTAMP NULL COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (handler_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_report_status (status),
    INDEX idx_report_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- 敏感词表
DROP TABLE IF EXISTS sensitive_words;
CREATE TABLE sensitive_words (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL UNIQUE COMMENT '敏感词内容',
    category VARCHAR(50) COMMENT '分类：POLITICS/PORN/AD/INSULT/OTHER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sensitive_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';

-- 精选/推荐内容表
DROP TABLE IF EXISTS featured_content;
CREATE TABLE featured_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型：ARTICLE/COLUMN/AUTHOR',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    position VARCHAR(50) NOT NULL DEFAULT 'HOME' COMMENT '展示位置：HOME/HOT_LIST/EDITOR_PICK',
    sort_order INT DEFAULT 0 COMMENT '排序权重，越大越靠前',
    curated_by BIGINT NOT NULL COMMENT '操作人（管理员ID）',
    start_at TIMESTAMP NULL COMMENT '推荐开始时间',
    end_at TIMESTAMP NULL COMMENT '推荐结束时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (curated_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_featured_position (position),
    INDEX idx_featured_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精选/推荐内容表';

-- 每日统计物化表
DROP TABLE IF EXISTS daily_statistics;
CREATE TABLE daily_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL COMMENT '统计日期',
    metric VARCHAR(50) NOT NULL COMMENT '指标名',
    value BIGINT DEFAULT 0 COMMENT '指标值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_date_metric (stat_date, metric),
    INDEX idx_daily_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计物化表';

-- 系统配置表
DROP TABLE IF EXISTS system_configs;
CREATE TABLE system_configs (
    config_key VARCHAR(100) PRIMARY KEY COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) COMMENT '配置说明',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 公告表
DROP TABLE IF EXISTS announcements;
CREATE TABLE announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容（支持Markdown）',
    type VARCHAR(20) DEFAULT 'NOTICE' COMMENT '类型：NOTICE-通知 MAINTENANCE-维护 ACTIVITY-活动',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿 PUBLISHED-已发布 ARCHIVED-已归档',
    publish_at TIMESTAMP NULL COMMENT '发布时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_announcement_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- 轮播图/Banner 表
DROP TABLE IF EXISTS banners;
CREATE TABLE banners (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) COMMENT 'Banner标题',
    image_url VARCHAR(500) NOT NULL COMMENT '图片地址',
    link_url VARCHAR(500) COMMENT '点击跳转链接',
    position VARCHAR(50) DEFAULT 'HOME' COMMENT '展示位置',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-启用 DISABLED-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_banner_position (position),
    INDEX idx_banner_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图/Banner表';

-- 管理员操作审计日志表
DROP TABLE IF EXISTS admin_logs;
CREATE TABLE admin_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL COMMENT '操作人（管理员ID）',
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    detail TEXT COMMENT '操作详情（JSON）',
    ip VARCHAR(50) COMMENT '操作者IP',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_admin_log_admin (admin_id),
    INDEX idx_admin_log_action (action),
    INDEX idx_admin_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作审计日志表';

-- 系统配置默认数据
INSERT INTO system_configs (config_key, config_value, description) VALUES
('site_name', '纸域空间平台', '站点名称'),
('hot_weight_views', '1', '热门文章：浏览量权重'),
('hot_weight_likes', '5', '热门文章：点赞数权重'),
('hot_weight_comments', '10', '热门文章：评论数权重'),
('content_review_enabled', 'false', '是否开启发布审核'),
('sensitive_filter_enabled', 'true', '是否开启敏感词过滤');

-- 敏感词默认数据
INSERT INTO sensitive_words (word, category) VALUES
('赌博', 'AD'),
('色情', 'PORN'),
('反动', 'POLITICS'),
('诈骗', 'AD');

-- ================================================
-- 匿名树洞表
-- ================================================
DROP TABLE IF EXISTS tree_hole_reports;
DROP TABLE IF EXISTS tree_hole_reactions;
DROP TABLE IF EXISTS tree_hole_replies;
DROP TABLE IF EXISTS tree_hole_posts;

CREATE TABLE tree_hole_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    anonymous_hash VARCHAR(64) NOT NULL COMMENT '匿名用户哈希',
    content TEXT NOT NULL COMMENT '心事内容',
    emotion_tag VARCHAR(30) NOT NULL DEFAULT 'other' COMMENT '情绪标签',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    empathy_count INT DEFAULT 0 COMMENT '拥抱数',
    reply_count INT DEFAULT 0 COMMENT '回复数',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    report_count INT DEFAULT 0 COMMENT '被举报次数',
    status INT DEFAULT 1 COMMENT '0-审核中 1-已发布 2-已拒绝 3-已删除',
    score DOUBLE DEFAULT 0 COMMENT '热度分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_thp_status_created (status, created_at),
    INDEX idx_thp_emotion (emotion_tag),
    INDEX idx_thp_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匿名树洞帖子表';

CREATE TABLE tree_hole_replies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL COMMENT '所属帖子ID',
    anonymous_hash VARCHAR(64) NOT NULL COMMENT '匿名用户哈希',
    content TEXT NOT NULL COMMENT '回复内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    status INT DEFAULT 1 COMMENT '1-正常 2-已拒绝 3-已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_thr_post (post_id),
    INDEX idx_thr_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匿名树洞回复表';

CREATE TABLE tree_hole_reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    anonymous_hash VARCHAR(64) NOT NULL COMMENT '匿名用户哈希',
    target_type VARCHAR(10) NOT NULL COMMENT '目标类型：POST/REPLY',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    reaction_type VARCHAR(10) NOT NULL COMMENT '反应类型：LIKE/EMPATHY',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_th_reaction (anonymous_hash, target_type, target_id, reaction_type),
    INDEX idx_threaction_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匿名树洞互动表';

CREATE TABLE tree_hole_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    anonymous_hash VARCHAR(64) NOT NULL COMMENT '匿名举报者哈希',
    target_type VARCHAR(10) NOT NULL COMMENT '目标类型：POST/REPLY',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    reason VARCHAR(500) COMMENT '举报原因',
    status INT DEFAULT 0 COMMENT '0-待处理 1-已处理 2-已忽略',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_th_report (anonymous_hash, target_type, target_id),
    INDEX idx_threport_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匿名树洞举报表';

-- 用户个性化设置表
CREATE TABLE user_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_settings_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户个性化设置表';

-- ================================================
-- 刷新令牌表
-- ================================================
DROP TABLE IF EXISTS refresh_tokens;
CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    token_hash CHAR(64) NOT NULL UNIQUE COMMENT '刷新令牌SHA-256摘要',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    revoked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已吊销',
    remembered TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否记住登录',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refresh_user (user_id),
    INDEX idx_refresh_expiry (expires_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌表';

-- ================================================
-- 验证查询
-- ================================================
SELECT '用户数据' as '表名', COUNT(*) as '数量' FROM users
UNION ALL
SELECT '文章数据', COUNT(*) FROM articles
UNION ALL
SELECT '评论数据', COUNT(*) FROM comments
UNION ALL
SELECT '标签数据', COUNT(*) FROM article_tags
UNION ALL
SELECT '专栏数据', COUNT(*) FROM `columns`
UNION ALL
SELECT '合集数据', COUNT(*) FROM collections;

-- ================================================
-- 用户建议反馈表（前台提交 → 后台按职位同步处理）
-- ================================================
DROP TABLE IF EXISTS user_feedback;
CREATE TABLE user_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL COMMENT '提交用户ID',
    type VARCHAR(20) NOT NULL COMMENT 'bug/feature/content/other',
    title VARCHAR(120) DEFAULT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '反馈内容',
    contact VARCHAR(120) DEFAULT NULL COMMENT '联系方式',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDLED',
    reply TEXT DEFAULT NULL COMMENT '处理回复',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_feedback_status (status),
    INDEX idx_user_feedback_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户建议反馈表';

-- 后台催办单（消息提示页使用，与前台私信隔离）
CREATE TABLE IF NOT EXISTS admin_reminder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE',
    reply VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL DEFAULT NULL,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    acknowledged_at TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_reminder_to (to_user_id, status),
    INDEX idx_reminder_from (from_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台催办单';
