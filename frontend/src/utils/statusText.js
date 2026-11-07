// 校园墙项目：状态翻译：后端返回的状态都是英文代号（NORMAL、DELETED 这些），页面上要显示汉语。
// 翻译只在前端做，后端接口返回的还是英文代号，不用改接口。

// 帖子状态：正常是 NORMAL；删了的要分是谁删的，作者自己删的是“已被用户删除”，管理员删的是“已被管理员删除”；
// 老数据以前删的时候没记是谁，就只显示“已删除”。
export function postStatusText(post) {
  if (!post) return ''
  if (post.status === 'NORMAL') return '正常'
  if (post.status === 'DELETED') {
    if (post.deletedBy === 'ADMIN') return '已被管理员删除'
    if (post.deletedBy === 'USER') return '已被用户删除'
    return '已删除'
  }
  return post.status
}

// 用户状态：ACTIVE 是正常账号；BANNED 是封禁，封禁到有日期就是临时封禁，没日期就是永久封禁；DELETED 是已注销。
export function userStatusText(user) {
  if (!user) return ''
  if (user.status === 'ACTIVE') return '正常'
  if (user.status === 'BANNED') return user.banUntil ? '临时封禁' : '永久封禁'
  if (user.status === 'DELETED') return '已注销'
  return user.status
}

// 举报状态：PENDING 是待处理，HANDLED 是已处理。
export function reportStatusText(status) {
  if (status === 'PENDING') return '待处理'
  if (status === 'HANDLED') return '已处理'
  return status || ''
}
