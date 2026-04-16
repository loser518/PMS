export const ROLE_OPTIONS = [
  { label: "学生", value: 0 },
  { label: "指导教师", value: 1 },
  { label: "管理员", value: 2 }
];

export const GENDER_OPTIONS = [
  { label: "女", value: 0 },
  { label: "男", value: 1 },
  { label: "未设置", value: 2 }
];

export const USER_STATUS_OPTIONS = [
  { label: "正常", value: 0 },
  { label: "封禁", value: 1 }
];

export const PROJECT_STATUS_OPTIONS = [
  { label: "待审核", value: 0 },
  { label: "审核通过", value: 1 },
  { label: "驳回", value: 2 },
  { label: "需修改", value: 3 }
];

export const PROGRESS_STATUS_OPTIONS = [
  { label: "待审核", value: 0 },
  { label: "导师已阅", value: 1 },
  { label: "驳回", value: 2 }
];

export const ANNOUNCEMENT_STATUS_OPTIONS = [
  { label: "草稿", value: 0 },
  { label: "已发布", value: 1 }
];

export const ANNOUNCEMENT_PRIORITY_OPTIONS = [
  { label: "普通", value: 0 },
  { label: "紧急", value: 1 },
  { label: "置顶", value: 2 }
];

export const TARGET_ROLE_OPTIONS = [
  { label: "全部", value: "ALL" },
  { label: "学生", value: "STUDENT" },
  { label: "指导教师", value: "TEACHER" }
];

export const COLOR_TYPE_OPTIONS = [
  { label: "success", value: "success" },
  { label: "warning", value: "warning" },
  { label: "danger", value: "danger" },
  { label: "info", value: "info" }
];

export const STUDENT_PROFILE_FIELDS = [
  { key: "grade", label: "年级" },
  { key: "major", label: "专业" },
  { key: "college", label: "学院" },
  { key: "className", label: "班级" },
  { key: "enrollmentYear", label: "入学年份" }
];

export const TEACHER_PROFILE_FIELDS = [
  { key: "title", label: "职称" },
  { key: "department", label: "系部/部门" },
  { key: "college", label: "学院" },
  { key: "researchField", label: "研究方向" },
  { key: "maxStudentCount", label: "最大指导人数" },
  { key: "currentStudentCount", label: "当前指导人数" }
];

export function getLabel(options, value, fallback = "-") {
  return options.find((item) => item.value === value)?.label ?? fallback;
}

export function summarizeProfile(profile, fields) {
  if (!profile) return "-";
  const values = fields
    .map((field) => profile[field.key])
    .filter((value) => value !== null && value !== undefined && value !== "");
  return values.length ? values.join(" / ") : "未完善";
}

export function getStatusTone(type, value) {
  const maps = {
    project: { 0: "warning", 1: "success", 2: "danger", 3: "info" },
    progress: { 0: "warning", 1: "success", 2: "danger" },
    announcement: { 0: "info", 1: "success", 2: "danger" },
    user: { 0: "success", 1: "danger", 2: "info" },
    priority: { 0: "info", 1: "warning", 2: "danger" }
  };
  return maps[type]?.[value] || "info";
}
