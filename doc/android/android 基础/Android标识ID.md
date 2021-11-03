尽管对用户隐私和广告追踪数据的尊重已经成为了近年 iOS 和 Android 系统功能更新中的重要一环，在 Google 缺席、鱼龙混杂的国内 Android 生态中，通过各种广告追踪手段获取个人隐私依然似探囊取物。今天我们要介绍的 OAID，便是众多广告追踪手段中最新的一种。

## ID 体系：你只是一串代码

想要了解 OAID，我们首先需要明白 ID 体系：**想要追踪一个用户就必须先找到用户**，在这个过程中，**标识符（ID）就像我们的另一张身份证，它们就代表了数字化之后的你和我。**

不同 App 可能通过某些唯一标识符对你进行强制跟踪，广告平台则会通过这个唯一标识符对你进行用户画像描绘，进而共享给相关 App 及其后台，一旦「你」打开了其中的某个 App，那么你就会被识别到——你点了什么、看过什么、可能需要什么，它们比你自己都清楚。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756352.png)

而在智能设备的 ID 体系中存在许许多多不同种类的标识符，下面是一小部分 Android 设备内的 ID，它们可能会用于不同方面的跟踪或标识：

### IMEI

IMEI 应该是大家最熟悉的一种 ID了，它是手机的身份证，也是运营商识别入网设备信息的代码，是一种不可重置的永久标识符，作用域为设备。

在广告跟踪方面，由于 iOS 的权限管控，iOS 上的第三方 App 并不能通过 IMEI 跟踪用户，但目前 Android 平台中绝大部分 App（尤其是在国内）都通过 IMEI 来追踪用户，开篇所举的例子在 Android 平台上大多也通过 IMEI 跟踪来实现。

与 IMEI 类似的还有一个叫做 IMSI 的标识符，但它主要用于 SIM 卡的身份标识，这里不做展开。

### Android ID（SSAID）

顾名思义，Android ID 是 Android 设备里不依赖于硬件的一种「半永久标识符」，在系统生命周期内不会改变，但系统重置或刷机后会发生变化，其作用域为一组有关联的应用。

[Android 开发者文档](https://developer.android.com/about/versions/oreo/android-8.0-changes.html#privacy-all)和[谷歌开发者中文博客](https://googledeveloperschina.blogspot.com/2017/04/android-o.html)对 Android 8.0 后的隐私性和 SSAID 变化做出了说明：

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756716.png)Android 开发者文档中对 Android 8.0 隐私性变化的说明

从图中不难看出，在 Android 8.0 以后，签名不同的 App 所获取的 Android ID（SSAID）是不一样的，但同一个开发者可以根据自己的数字签名，将所开发的不同 App 进行关联。

### Device ID

在 Android 平台，Device ID 是一种统称，与硬件相关的 ID 都可以称之为 Device ID，一般是一种不可重置的永久标识符，作用域为设备。

根据设备、厂家或者 App 调用需求的不同，读取 Device ID 时可能会返回 IMEI 或其他硬件编码，但也有可能因为设备中没有相关硬件而无法获取 Device ID 或返回无效值；与之形成对应的，iOS 设备中也有类似的永久标识符叫做 UDID，但在 iOS 6 之后，苹果已经不允许需要获取 UDID 的 App 上架 App Store 以防止这种不可重置的 ID 被用于追踪或滥用，取而代之的是 IDFA 标识符，即 iOS 设备广告标识符。

另外还有一种叫做 openUDID 的设备唯一标识符，它在 iOS 和 Android 系统内都可以使用，但由于不是系统官方提供的 ID 体系，且依赖于第三方 App 生成，所以应用并不广泛，而随着系统迭代升级，openUDID 也逐渐被边缘化甚至被废弃。

### UUID、GUID

UUID 也叫做实例 ID，这两个 ID 可以说是在计算机体系内的通用标识符（详细了解 UUID 和 GUID 可以阅读 [维基百科](https://zh.wikipedia.org/zh-hans/通用唯一识别码) 相关内容）。

根据所面向对象的不同，其意义也有微小差别。如果说前面三个 ID 可以用来识别设备，那么这两个 ID 在 Android 系统中的作用主要是识别 App 进程、元素或数据。

因为它们的作用域仅仅是单个应用内，如果用户卸载了该 App 并重新安装，那么 UUID 也会发生变化。不过 App 开发者可以通过存储 UUID 或与其他 ID、用户信息进行组合、绑定、计算等方式，实现 UUID 标识符的「准永久化」。

根据 [Android 开发者指南](https://developer.android.com/training/articles/user-data-ids?hl=zh-cn#instance-ids-guids)：

> 标识运行在设备上的应用实例最简单明了的方法就是使用实例 ID，在大多数非广告用例中，这是建议的解决方案。只有进行了针对性配置的应用实例才能访问该标识符，并且标识符重置起来（相对）容易，因为它只存在于应用的安装期。
>
> 因此，与无法重置的设备级硬件 ID 相比，实例 ID 具有更好的隐私权属性。

### AAID

AAID 与 IDFA 作用相同——IDFA 是 iOS 平台内的广告跟踪 ID，[AAID 则用于 Android 平台](https://support.google.com/authorizedbuyers/answer/3221407?hl=zh-Hans)。

它们都是一种非永久、可重置的标识符，专门提供给 App 以进行广告行为，用户随时可以重置该类 ID，或通过系统设置关闭个性化广告跟踪。但 AAID 依托于 Google 服务框架，因此如果手机没有内置该框架、或框架不完整、或无法连接到相关服务，这些情况都有可能导致 AAID 不可用。

除了以上这些 ID 标识符以外，某些硬件 ID（例如 MAC 地址）也可能会被用于追踪。

## 国内 Android 的广告追踪之道

这么多 ID 标识符，每一个都各司其职。**而理论上来说，只有 AAID 和 IDFA 是真正用于广告行为的。**

但现实状况显然不是这样。

一方面，Android 平台的不少 App 普遍存在违反 Android 开发规范、[绕过 Google Play 审查](https://nakedsecurity.sophos.com/2019/02/19/thousands-of-android-apps-bypass-advertising-id-to-track-users/)，通过滥用 ID 来追踪用户，以此达到为广告流量、营销分析等商业利益服务的目的。

另一方面，由于 AAID 依托于 Google 服务框架，但在国内使用 Google 服务并不太可行，或者大部分国行手机内置的 Google 服务不完整，App 开发者需要寻找另一个方式去标识用户。

UUID、GUID 作用域太小，不适合广告跟踪；Android ID 可以通过某些方式被改变或因为 bug 导致不可用，第三方 App 无保证可用性；MAC 地址虽然精准，但在Android 6.0（API 23）到 Android 9（API 28）中，系统限制了第三方 API 获取MAC 地址；再加上早些时候，大部分「非玩机用户」对此类功能并没有太多概念，第三方 App 为了能以更加精准持久的方式来跟踪用户，**将 IMEI 变成了用于广告跟踪的首选 ID**（在 [Google Play 帮助中心](https://support.google.com/googleplay/android-developer/answer/6048248?hl=zh-Hans)，获取永久标识符是一种有条件的、退而求其次的广告投放方法，所以在此之前这种方式也不算完全违规）。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756489.png)Google Play 帮助文档中的相关说明

这也是我们看到很多 App 提示必须获取「电话」权限才能运行的原因——**因为获取 IMEI 必须获得「电话」权限**，可是由此也带来了一些隐私问题：允许「电话」权限可能导致 App 读取到很多种其他信息，就像我需要你给我身份证来查询身份证号，但与此同时你的姓名、住址、生日也暴露给我了。

我们可以在 [这个网站](https://search.appcensus.io/) 查询到部分 App 所需要的权限、资源以及它可能会发送的隐私信息。例如微信，在网站中收录的 6.7.3 版本中，微信获取了如下权限，但并没有检测到发送以下隐私数据。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221757862.png)微信 6.7.3 版本所需的权限

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756796.png)微信 6.7.3 版本并没有发送以上隐私信息（灰色虚线框标识未检测到）

随着时代发展，用户逐渐认识到手机 App 疯狂获取权限的行为有可能会侵犯隐私，加之近几年 Android 系统的权限和隐私管理逐渐收紧，Android 10（API 29）终于对第三方 App 获取不可重置永久设备标识符（包括 IMEI）的行为做出了 [限制](https://developer.android.com/about/versions/10/privacy/changes)。

具体到用户层面，在 Android 10 之后应用即便能够获取到「电话」权限，系统返回给应用的 IMEI 信息值也为空（你可以通过 [My IMEI](https://play.google.com/store/apps/details?id=marcus.myimei) 这款应用进行测试）。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756620.png)Android 10 中应用无法获取 IMEI 信息

## OAID：Android 10 之后的替代方案

至此，国内 App 和广告跟踪服务急需一种替代方案以避免广告流量的损失，OAID 顺势而生。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756645.png)Android 开发者文档中对 Android 10 限制设备标识符读取的说明

Android 开发者文档中对 Android 10 限制设备标识符读取的说明 OAID 的本质其实是一种在国行系统内使用的、应对 Android 10 限制读取 IMEI 的、「拯救」国内移动广告的广告跟踪标识符，其背后是 [移动安全联盟](http://msa-alliance.cn/)（Mobile Security Alliance，简称 MSA）。

该联盟由中国信息通信研究院担任理事长和秘书长单位，北京大学、vivo、360、华为担任副理事长单位，并有包括苹果、中兴、OPPO、小米等多家理事和会员单位（[点击查看联盟成员详情](http://msa-alliance.cn/col.jsp?id=105)），OAID 所属的标识符体系也是由该联盟牵头发起的（参见「[移动智能终端补充设备标识体系](http://msa-alliance.cn/col.jsp?id=120)」）。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756377.jpeg)补充设备标识体系图示

根据联盟官网以及开发文档，这个「本土化」标识符体系除了 OAID，还包含 UDID、VAID 和 AAID 一共四种标识符。

**我知道你在想什么，不过这里的 UDID 和 AAID 与上一节所说的完全不同**。你可以通过下图来了解「移动智能终端补充设备标识体系」所规定的四种标识符以及获取它们的接口开发方式。另外，你也可以在 MSA 官网或会员单位的开发者网站下载 SDK 开发说明。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756463.png)补充设备标识体系

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221757007.png)补充设备标识体系获取接口

从这四种标识符的描述和功能我们大致可以确定，「移动智能终端补充设备标识体系」所规定的 UDID、OAID、VAID、AAID 在 Android 系统中分别对应了 Device ID（例如 IMEI，或对应了 iOS 设备的 UDID）、AAID、SSAID、UUID（或 GUID）。在理想状态下，引入 OAID 即能保证广告平台的正常运作，也能减小对用户带来的影响，因为第三方 App 无需请求权限即可使用 OAID 完成广告行为，而该过程匿名，用户也可以随时重置 OAID。

从广告服务商 Adjust 于 2019 年12 月 20 日发布的 [新闻稿](https://www.adjust.com/product-updates/adjust-supports-oaid-for-tracking-in-china/) 中我们也可以获知，Adjust 已经接入了 OAID 广告标识符，能够对中国大陆的广告主提供广告相关服务；国内广告数据服务商神策数据在其 [帮助文档](http://manual.sensorsdata.cn/sa/latest/page-7536677.html) 中同样提供了 OAID 匹配指南；华为则在其 [开发者平台](https://developer.huawei.com/consumer/cn/codelab/HMSAdsOAID/index.html#0) 提供了基于 OAID 的「HUAWEI Ads OAID」广告平台接入指南。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756811.png)神策数据在帮助文档中对 OAID 的简要介绍

事实上，主流手机厂商都已经在其开发者平台上提供了 Android 10 适配指引，包括 [三星中国开发者网站](https://support-cn.samsung.com/App/DeveloperChina/notice/detail?noticeid=115)、[华为开发者联盟](https://developer.huawei.com/consumer/cn/doc/50127)、[OPPO 开放平台](https://open.oppomobile.com/wiki/doc#id=10432)、[vivo 开放平台](https://dev.vivo.com.cn/documentCenter/doc/235) 都已针对 Android 10 的相关变化做出了说明和解决方案建议，**其中就包括 Google 官方标识符适配建议和 OAID 适配方式**。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221757075.png)OPPO 开放平台引用 Android 开发者文档作为推荐适配方案之一

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110221756163.png)三星中国开发者网站中引用 MSA 说明文档作为推荐方案之一

另外 [OPPO](https://open.oppomobile.com/wiki/doc#id=10608) 和 [vivo](https://dev.vivo.com.cn/documentCenter/doc/253) 也分别在其开放平台提供了「移动智能终端补充设备标识体系」相关文档和 SDK 下载。

不难看出，广告平台已经开始接入 OAID 作为国内广告标识符的建议方案，主流设备厂家也已经开始指导开发者采用「移动智能终端补充设备标识体系」，并且考虑到国内主流的应用预置和分发平台（例如手机厂商内建的应用商店）与 Google Play 一样开始对上架 App 的 API 等级做出强制要求，包括 OAID 在内的标识符体系毫无疑问将成为国内第三方 App 的强制执行标准。