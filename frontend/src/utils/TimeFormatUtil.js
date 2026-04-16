/**
 * 时间格式化工具函数
 */
export const timeUtil = {
    /**
     * 格式化日期
     */

    /**
     * 时间格式化工具函数
     * @param {Date|string|number} date - 日期对象、时间字符串或时间戳
     * @param {string} format - 格式化模板，默认 'YYYY-MM-DD HH:mm:ss'
     * @returns {string} 格式化后的时间字符串
     */
    formatDate(date = new Date(), format = 'YYYY-MM-DD HH:mm:ss') {
        // 处理输入
        let d = new Date(date)

        // 检查日期是否有效
        if (isNaN(d.getTime())) {
            console.error('Invalid date input:', date)
            return 'Invalid Date'
        }

        const map = {
            // 年
            'YYYY': d.getFullYear(),
            'YY': String(d.getFullYear()).slice(-2),

            // 月
            'MM': String(d.getMonth() + 1).padStart(2, '0'),
            'M': d.getMonth() + 1,

            // 日
            'DD': String(d.getDate()).padStart(2, '0'),
            'D': d.getDate(),

            // 时
            'HH': String(d.getHours()).padStart(2, '0'),
            'H': d.getHours(),
            'hh': String(d.getHours() % 12 || 12).padStart(2, '0'),
            'h': d.getHours() % 12 || 12,

            // 分
            'mm': String(d.getMinutes()).padStart(2, '0'),
            'm': d.getMinutes(),

            // 秒
            'ss': String(d.getSeconds()).padStart(2, '0'),
            's': d.getSeconds(),

            // 毫秒
            'SSS': String(d.getMilliseconds()).padStart(3, '0'),
            'S': d.getMilliseconds(),

            // 季度
            'Q': Math.floor((d.getMonth() + 3) / 3),

            // 上午/下午
            'A': d.getHours() < 12 ? 'AM' : 'PM',
            'a': d.getHours() < 12 ? 'am' : 'pm'
        }

        return format.replace(/YYYY|YY|MM|M|DD|D|HH|H|hh|h|mm|m|ss|s|SSS|S|Q|A|a/g, (match) => {
            return map[match] !== undefined ? map[match] : match
        })
    },

    /**
     * 获取相对时间描述（几分钟前、几小时前、几天前等）
     * @param {Date|string|number} date - 目标时间
     * @param {Date|string|number} [now] - 参考时间，默认当前时间
     * @returns {string} 相对时间描述
     */
    fromNow(date, now = new Date()) {
        const target = new Date(date).getTime()
        const current = new Date(now).getTime()

        if (isNaN(target) || isNaN(current)) {
            return 'Invalid Date'
        }

        const diff = current - target
        const seconds = Math.floor(diff / 1000)
        const minutes = Math.floor(seconds / 60)
        const hours = Math.floor(minutes / 60)
        const days = Math.floor(hours / 24)
        const months = Math.floor(days / 30)
        const years = Math.floor(months / 12)

        if (years > 0) return `${years}年前`
        if (months > 0) return `${months}个月前`
        if (days > 0) return `${days}天前`
        if (hours > 0) return `${hours}小时前`
        if (minutes > 0) return `${minutes}分钟前`
        if (seconds > 10) return `${seconds}秒前`
        return '刚刚'
    },

    /**
     * 获取友好的日期显示（今天、明天、昨天等）
     * @param {Date|string|number} date - 目标时间
     * @returns {string} 友好日期描述
     */
    friendly(date) {
        const target = new Date(date)
        const today = new Date()

        // 设置为当天的0点进行比较
        const targetDate = new Date(target.getFullYear(), target.getMonth(), target.getDate())
        const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())

        const diffDays = Math.floor((targetDate - todayDate) / (1000 * 60 * 60 * 24))

        if (diffDays === 0) return '今天'
        if (diffDays === 1) return '明天'
        if (diffDays === -1) return '昨天'
        if (diffDays > 1 && diffDays < 7) return `${diffDays}天后`
        if (diffDays < -1 && diffDays > -7) return `${-diffDays}天前`

        return this.format(target, 'MM月DD日')
    },

    /**
     * 获取开始和结束时间的时间范围
     * @param {string} range - 时间范围：today, yesterday, thisWeek, lastWeek, thisMonth, lastMonth
     * @returns {Object} 包含 start 和 end 的日期对象
     */
    getRange(range) {
        const now = new Date()
        const start = new Date(now)
        const end = new Date(now)

        switch (range) {
            case 'today':
                start.setHours(0, 0, 0, 0)
                end.setHours(23, 59, 59, 999)
                break
            case 'yesterday':
                start.setDate(start.getDate() - 1)
                start.setHours(0, 0, 0, 0)
                end.setDate(end.getDate() - 1)
                end.setHours(23, 59, 59, 999)
                break
            case 'thisWeek':
                start.setDate(start.getDate() - start.getDay() + 1) // 周一
                start.setHours(0, 0, 0, 0)
                end.setDate(end.getDate() + (7 - end.getDay())) // 周日
                end.setHours(23, 59, 59, 999)
                break
            case 'thisMonth':
                start.setDate(1)
                start.setHours(0, 0, 0, 0)
                end.setMonth(end.getMonth() + 1, 0)
                end.setHours(23, 59, 59, 999)
                break
            case 'lastMonth':
                start.setMonth(start.getMonth() - 1, 1)
                start.setHours(0, 0, 0, 0)
                end.setMonth(now.getMonth(), 0)
                end.setHours(23, 59, 59, 999)
                break
            default:
                return {start: null, end: null}
        }

        return {start, end}
    },

    /**
     * 获取时间戳（秒级）
     */
    timestamp(date = new Date()) {
        return Math.floor(new Date(date).getTime() / 1000)
    },

    /**
     * 检查是否是同一天
     */
    isSameDay(date1, date2 = new Date()) {
        const d1 = new Date(date1)
        const d2 = new Date(date2)
        return d1.getFullYear() === d2.getFullYear() &&
            d1.getMonth() === d2.getMonth() &&
            d1.getDate() === d2.getDate()
    },

    /**
     * 获取年龄（根据生日）
     * @param {Date|string|number} birthday - 生日
     * @returns {number} 年龄
     */
    getAge(birthday) {
        const birth = new Date(birthday)
        const today = new Date()

        let age = today.getFullYear() - birth.getFullYear()
        const monthDiff = today.getMonth() - birth.getMonth()

        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
            age--
        }

        return age
    }
}
