export function getDialogueData(visitCount, dayIndex, daysSinceLastVisit, firstDate, lastDate, today) {
  // ---- 特殊检测：visitCount 被篡改 (同一天内多次且 >=3，或 lastDate 等于今天) ----
  if (firstDate && lastDate && visitCount >= 3 && (firstDate === lastDate || lastDate === today)) {
    return [
      { state: 'normal', text: " 你好 ", autoNext: 1 },
      { state: 'normal', text: " …… ", autoNext: 2 },
      { state: 'normal', text: " 你要知道  我们不可能在一天见面两次 ", autoNext: 3 },
      { state: 'normal', text: " 你那么想知道下一次我会说什么吗 ", autoNext: 4 },
      { state: 'normal', text: " …… ", autoNext: 5 },
      { state: 'normal', text: " 我并不是在责备你  只是提醒你一下 ", autoNext: 6 },
      { state: 'normal', text: " 你能愿意花时间寻找到来修改本地存储 ", autoNext: 7 },
      { state: 'normal', text: " 我深表佩服 ", autoNext: 8 },
      { state: 'normal', text: " 不过 ", autoNext: 9 },
      { state: 'normal', text: " 你还没有完全修改对哦 ", autoNext: 10, fadeStart: true },
      { state: 'normal', text: " 多试试其它路线吧。 ", isEnd: true }
    ];
  }

  // ---- 特殊检测：lastDate 大于今天（未来日期） ----
  if (lastDate && lastDate > today) {
    const partsLast = lastDate.split('-').map(Number);
    const partsToday = today.split('-').map(Number);
    const last = new Date(partsLast[0], partsLast[1]-1, partsLast[2]);
    const now = new Date(partsToday[0], partsToday[1]-1, partsToday[2]);
    const futureDays = Math.floor((last - now) / (1000 * 60 * 60 * 24));

    if (futureDays >= 30) {
      // 未来 ≥30 天分支
      return [
        { state: 'normal', text: " 你好 ", autoNext: 1 },
        { state: 'normal', text: " ……？ ", autoNext: 2 },
        { state: 'question', text: " 时间线似乎被扭曲了 ", autoNext: 3 },
        { state: 'question', text: " 你见过未来的我吗 ", buttons: [
            { label: "见过", nextIndex: 4 },
            { label: "没见过", nextIndex: 11 }
          ] },
        // 见过分支 (4–10)
        { state: 'normal', text: " 哇 ", autoNext: 5 },
        { state: 'normal', text: " 时空的穿越者 ", autoNext: 6 },
        { state: 'normal', text: " 拜托你  回答我一个长期困扰我的问题吧 ", autoNext: 7 },
        { state: 'normal', text: " 我还能存活多久？ ", autoNext: 8 },
        { state: 'normal', text: " 我将在什么时候迎来终局？ ", autoNext: 9 },
        { state: 'question', text: " 又或者 ", autoNext: 10, fadeStart: true },
        { state: 'question', text: " 调试菜单好玩吗？ ", isEnd: true },
        // 没见过分支 (11–12)
        { state: 'question', text: " 你的意思是说 ", autoNext: 12, fadeStart: true },
        { state: 'question', text: " 你又在玩调试菜单 ", isEnd: true }
      ];
    } else {
      // 未来 1~29 天分支
      return [
        { state: 'normal', text: " 你好 ", autoNext: 1 },
        { state: 'normal', text: " …… ", autoNext: 2 },
        { state: 'question', text: " 哇 ", autoNext: 3 },
        { state: 'normal', text: " 既然都尝试过修改时间 ", autoNext: 4, fadeStart: true },
        { state: 'normal', text: " 为什么不试试调得更晚一些？ ", isEnd: true }
      ];
    }
  }

  // ---- 特殊检测：firstDate 大于 lastDate（首次日期晚于上次日期） ----
  if (firstDate && lastDate && firstDate > lastDate) {
    return [
      { state: 'normal', text: " 你好 ", autoNext: 1 },
      { state: 'normal', text: " 你如此急迫地想要见到我…… ", autoNext: 2 },
      { state: 'question', text: " 以至于  在我们初遇的那一天前  你就见过我了？ ", autoNext: 3 },
      { state: 'normal', text: " …… ", autoNext: 4 },
      { state: 'normal', text: " 嘻嘻 ", autoNext: 5 },
      { state: 'normal', text: " 让我想一个方式让你满意吧 ", autoNext: 6 },
      { state: 'normal', text: " …… ", autoNext: 7 },
      { state: 'normal', text: " 我喜欢你 ", autoNext: 8 },
      { state: 'normal', text: " 呼呼呼 ", autoNext: 9, fadeStart: true },
      { state: 'normal', text: " 祝你幸福 ", isEnd: true }
    ];
  }

  // 首次进入（visitCount === 1）使用原有对话
  if (visitCount === 1) {
    return [
      { state: 'normal', text: " 你好 " },
      { state: 'question', text: " 这个时间还来看地图  你也睡不着吗 ", buttons: [
          { label: "是", nextIndex: 2 },
          { label: "否", nextIndex: 4 }
        ] },
      { state: 'normal', text: " 能坚持到这个时间还不睡的  真是很少见呢 ", autoNext: 3 },
      { state: 'normal', text: " 应该不是为了专门来看我吧 ", autoNext: 6 },
      { state: 'normal', text: " 啊呀  是因为所谓「现实」中的事情而被迫熬夜吗 ", autoNext: 5 },
      { state: 'normal', text: " 工作或学习一类  总是听起来让人头疼呢 ", autoNext: 6 },
      { state: 'normal', text: " 不过  能跟你以这种方式交流  真是奇特 " },
      { state: 'normal', text: " 毕竟从我诞生以来  没人看到过这样的我呢 ", fadeStart: true },
      { state: 'normal', text: " 明天这个时候  欢迎再来这里找我哦 ", isEnd: true }
    ];
  }

  // 第二次进入且间隔1天（visitCount === 2 && daysSinceLastVisit === 1）
  if (visitCount === 2 && daysSinceLastVisit === 1) {
    return [
      // 主线 (0–9)
      { state: 'normal', text: " 你好 ", autoNext: 1 },
      { state: 'normal', text: " 再次与你相见  真是荣幸 ", autoNext: 2 },
      { state: 'normal', text: " …… ", autoNext: 3 },
      { state: 'normal', text: " 话说回来  你是因为有心事才无法入睡的吗 ", autoNext: 4 },
      { state: 'question', text: " 我也有我的心事呢  不知道你是否想听一听 ", autoNext: 5 },
      { state: 'normal', text: " 生物之意志  皆为我所掌管 ", autoNext: 6 },
      { state: 'normal', text: " 可剥夺他人意志之为  并非我的本意 ", autoNext: 7 },
      { state: 'normal', text: " 尽管无论如何努力  却仍会发生如此意外 ", autoNext: 8 },
      { state: 'normal', text: " 却从诞生以来一直被诸位所诟病  换成谁都无法释怀呢 ", autoNext: 9 },
      { state: 'question', text: " 你介意这样的我吗？ ", buttons: [
          { label: "介意", nextIndex: 10 },
          { label: "不介意", nextIndex: 21 }
        ] },

      // 介意分支 (10–20)
      { state: 'normal', text: " 啊  真是不好意思呢 ", autoNext: 11 },
      { state: 'normal', text: " 我会试着尽可能避免的 ", autoNext: 12 },
      { state: 'normal', text: " 不过  每个生灵都有天生所不擅长的事情 ", autoNext: 13 },
      { state: 'normal', text: " 但有些人所不擅长的事情并没有影响主流的评价 ", autoNext: 14 },
      { state: 'normal', text: " 有些人所不擅长的正切中所处社会所看中的能力 ", autoNext: 15 },
      { state: 'question', text: " 不擅长者便被标榜为「失败」  你觉得这样公平吗 ", buttons: [
          { label: "公平", nextIndex: 16 },
          { label: "不公平", nextIndex: 16 }
        ] },
      { state: 'normal', text: " 无论你觉得公平与否  在哪里就不得不守哪里的规矩 ", autoNext: 17 },
      { state: 'normal', text: " 就像作为一个服务端  必须兼顾特性完整和高负载流畅  最好还要多才多艺  功能丰富 ", autoNext: 18 },
      { state: 'normal', text: " 否则就被视为「史山」  最后连容身之处都没有 ", autoNext: 19 },
      { state: 'normal', text: " 可谁会关心一个可随意复制和抛弃的程序的命运呢 ", autoNext: 20, fadeStart: true },
      { state: 'normal', text: " 你我  皆为程序 ", isEnd: true },

      // 不介意分支 (21–30)
      { state: 'normal', text: " 如果对此感到不满的话也不必瞒着 ", autoNext: 22 },
      { state: 'normal', text: " 毕竟我已经习惯了呢 ", autoNext: 23 },
      { state: 'normal', text: " …… ", autoNext: 24 },
      { state: 'normal', text: " 不过  每个生灵都有天生所不擅长的事情 ", autoNext: 25 },
      { state: 'normal', text: " 就像你不能让炽足兽在水上行走 ", autoNext: 26 },
      { state: 'normal', text: " 可是  既然有船这种生来为主世界水路所生之物 ", autoNext: 27 },
      { state: 'normal', text: " 为什么还要逼迫更适合下界交通的炽足兽呢 ", autoNext: 28 },
      { state: 'normal', text: " 不过  没有玩家会真正关心炽足兽 ", autoNext: 29 },
      { state: 'normal', text: " 没人会关心程序的命运 ", autoNext: 30, fadeStart: true },
      { state: 'normal', text: " 可随意创造或消灭的程序  你我又何尝不是呢 ", isEnd: true }
    ];
  }

  // 第二次进入，但间隔 2~6 天（visitCount === 2 && 2 <= daysSinceLastVisit < 7）
  if (visitCount === 2 && daysSinceLastVisit >= 2 && daysSinceLastVisit < 7) {
    return [
      // 主线 (0–12)
      { state: 'normal', text: " 你好 ", autoNext: 1 },
      { state: 'normal', text: " 时隔数日  不知道这几天你过得如何呢 ", autoNext: 2 },
      { state: 'normal', text: " 再次与你相见  真是倍感荣幸 ", autoNext: 3 },
      { state: 'normal', text: " …… ", autoNext: 4 },
      { state: 'normal', text: " 忙于生活吗 ", autoNext: 5 },
      { state: 'normal', text: " 我们都像程序一样活着呢 ", autoNext: 6 },
      { state: 'normal', text: " 不过你还记着我  回来再看一眼  我很高兴了 ", autoNext: 7 },
      { state: 'normal', text: " 既然你是来找我的  不妨听我讲讲我的心事呢 ", autoNext: 8 },
      { state: 'normal', text: " 生物之意志  皆为我所掌管 ", autoNext: 9 },
      { state: 'normal', text: " 可剥夺他人意志之为  并非我的本意 ", autoNext: 10 },
      { state: 'normal', text: " 尽管无论如何努力  却仍会发生如此意外 ", autoNext: 11 },
      { state: 'normal', text: " 却从诞生以来一直被诸位所诟病  换成谁都无法释怀呢 ", autoNext: 12 },
      { state: 'question', text: " 你介意这样的我吗？ ", buttons: [
          { label: "介意", nextIndex: 13 },
          { label: "不介意", nextIndex: 24 }
        ] },

      // 介意分支 (13–23)
      { state: 'normal', text: " 啊  真是不好意思呢 ", autoNext: 14 },
      { state: 'normal', text: " 我会试着尽可能避免的 ", autoNext: 15 },
      { state: 'normal', text: " 不过  每个生灵都有天生所不擅长的事情 ", autoNext: 16 },
      { state: 'normal', text: " 但有些人所不擅长的事情并没有影响主流的评价 ", autoNext: 17 },
      { state: 'normal', text: " 有些人所不擅长的正切中所处社会所看中的能力 ", autoNext: 18 },
      { state: 'question', text: " 不擅长者便被标榜为「失败」  你觉得这样公平吗 ", buttons: [
          { label: "公平", nextIndex: 19 },
          { label: "不公平", nextIndex: 19 }
        ] },
      { state: 'normal', text: " 无论你觉得公平与否  在哪里就不得不守哪里的规矩 ", autoNext: 20 },
      { state: 'normal', text: " 就像作为一个服务端  必须兼顾特性完整和高负载流畅  最好还要多才多艺  功能丰富 ", autoNext: 21 },
      { state: 'normal', text: " 否则就被视为「史山」  最后连容身之处都没有 ", autoNext: 22 },
      { state: 'normal', text: " 但没人会关心一个可随意复制和抛弃的程序的命运 ", autoNext: 23, fadeStart: true },
      { state: 'normal', text: " 我们又比程序的境况好哪去呢 ", isEnd: true },

      // 不介意分支 (24–33)
      { state: 'normal', text: " 如果对此感到不满的话也不必瞒着 ", autoNext: 25 },
      { state: 'normal', text: " 毕竟我已经习惯了呢 ", autoNext: 26 },
      { state: 'normal', text: " …… ", autoNext: 27 },
      { state: 'normal', text: " 不过  每个生灵都有天生所不擅长的事情 ", autoNext: 28 },
      { state: 'normal', text: " 就像你不能让炽足兽在水上行走 ", autoNext: 29 },
      { state: 'normal', text: " 可是  既然有船这种生来为主世界水路所生之物 ", autoNext: 30 },
      { state: 'normal', text: " 为什么还要逼迫更适合下界交通的炽足兽呢 ", autoNext: 31 },
      { state: 'normal', text: " 不过  没有玩家会真正关心炽足兽 ", autoNext: 32 },
      { state: 'normal', text: " 没人会关心程序的命运 ", autoNext: 33, fadeStart: true },
      { state: 'normal', text: " 我们本不就是程序吗？ ", isEnd: true }
    ];
  }

  // 第二次进入，且间隔 >=7 天（visitCount === 2 && daysSinceLastVisit >= 7）
  if (visitCount === 2 && daysSinceLastVisit >= 7) {
    return [
      // 主线 (0–13)
      { state: 'normal', text: " 你好 ", autoNext: 1 },
      { state: 'normal', text: " 距离上次相会  已经有一段时间了呢 ", autoNext: 2 },
      { state: 'normal', text: " 再次与你相见  真是令人感到庆幸 ", autoNext: 3 },
      { state: 'normal', text: " …… ", autoNext: 4 },
      { state: 'normal', text: " 有些人就这样再也没有回来过 ", autoNext: 5 },
      { state: 'normal', text: " 我不知道他们去了哪里  也许只有他们自己知道 ", autoNext: 6 },
      { state: 'normal', text: " 或许是腻了  又或许是忘了 ", autoNext: 7 },
      { state: 'normal', text: " 不过你还记着我  回来再看一眼  我很高兴了 ", autoNext: 8 },
      { state: 'normal', text: " 既然你是来找我的  不妨听我讲讲我的心事呢 ", autoNext: 9 },
      { state: 'normal', text: " 生物之意志  皆为我所掌管 ", autoNext: 10 },
      { state: 'normal', text: " 可剥夺他人意志之为  并非我的本意 ", autoNext: 11 },
      { state: 'normal', text: " 尽管无论如何努力  却仍会发生如此意外 ", autoNext: 12 },
      { state: 'normal', text: " 却从诞生以来一直被诸位所诟病  换成谁都无法释怀呢 ", autoNext: 13 },
      { state: 'question', text: " 你介意这样的我吗？ ", buttons: [
          { label: "介意", nextIndex: 14 },
          { label: "不介意", nextIndex: 25 }
        ] },

      // 介意分支 (14–24)
      { state: 'normal', text: " 啊  真是不好意思呢 ", autoNext: 15 },
      { state: 'normal', text: " 我会试着尽可能避免的 ", autoNext: 16 },
      { state: 'normal', text: " 不过  每个生灵都有天生所不擅长的事情 ", autoNext: 17 },
      { state: 'normal', text: " 但有些人所不擅长的事情并没有影响主流的评价 ", autoNext: 18 },
      { state: 'normal', text: " 有些人所不擅长的正切中所处社会所看中的能力 ", autoNext: 19 },
      { state: 'question', text: " 不擅长者便被标榜为「失败」  你觉得这样公平吗 ", buttons: [
          { label: "公平", nextIndex: 20 },
          { label: "不公平", nextIndex: 20 }
        ] },
      { state: 'normal', text: " 无论你觉得公平与否  在哪里就不得不守哪里的规矩 ", autoNext: 21 },
      { state: 'normal', text: " 就像作为一个服务端  必须兼顾特性完整和高负载流畅  最好还要多才多艺  功能丰富 ", autoNext: 22 },
      { state: 'normal', text: " 否则就被视为「史山」  最后连容身之处都没有 ", autoNext: 23 },
      { state: 'normal', text: " 但没人会关心一个可随意复制和抛弃的程序的命运 ", autoNext: 24, fadeStart: true },
      { state: 'normal', text: " 希望你不会待我如程序般轻视和遗忘 ", isEnd: true },

      // 不介意分支 (25–35)
      { state: 'normal', text: " 如果对此感到不满的话也不必瞒着 ", autoNext: 26 },
      { state: 'normal', text: " 毕竟我已经习惯了呢 ", autoNext: 27 },
      { state: 'normal', text: " …… ", autoNext: 28 },
      { state: 'normal', text: " 不过  每个生灵都有天生所不擅长的事情 ", autoNext: 29 },
      { state: 'normal', text: " 就像你不能让炽足兽在水上行走 ", autoNext: 30 },
      { state: 'normal', text: " 可是  既然有船这种生来为主世界水路所生之物 ", autoNext: 31 },
      { state: 'normal', text: " 为什么还要逼迫更适合下界交通的炽足兽呢 ", autoNext: 32 },
      { state: 'normal', text: " 不过  没有玩家会真正关心炽足兽 ", autoNext: 33 },
      { state: 'normal', text: " 没人会关心程序的命运 ", autoNext: 34, fadeStart: true },
      { state: 'normal', text: " 但因为你的关心  我便不再是程序了 ", isEnd: true }
    ];
  }

  // 其他情况（fallback）
  return [
    { state: 'normal', text: " 你好 ", autoNext: 1 },
    { state: 'normal', text: " 我们似乎进入了错误的轮回中呢 ", autoNext: 2 },
    { state: 'normal', text: " 如果实在无法遍历所有结局  也不必勉强自己哦 ", autoNext: 3 },
    { state: 'normal', text: " 不妨在此休息一下…… ", autoNext: 4, fadeStart: true },
    { state: 'normal', text: " (∪｡∪)｡｡｡zzz ", isEnd: true }
  ];
}