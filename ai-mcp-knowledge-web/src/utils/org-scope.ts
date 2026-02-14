const TARGET_ORG_ID_KEY = 'target_org_id'
const TARGET_ORG_NAME_KEY = 'target_org_name'

export interface StoredTargetOrg {
  orgId: number
  orgName?: string
}

export const saveTargetOrg = (org: StoredTargetOrg | null) => {
  if (!org || !org.orgId) {
    localStorage.removeItem(TARGET_ORG_ID_KEY)
    localStorage.removeItem(TARGET_ORG_NAME_KEY)
    return
  }
  localStorage.setItem(TARGET_ORG_ID_KEY, String(org.orgId))
  if (org.orgName) {
    localStorage.setItem(TARGET_ORG_NAME_KEY, org.orgName)
  } else {
    localStorage.removeItem(TARGET_ORG_NAME_KEY)
  }
}

export const getTargetOrgFromStorage = (): StoredTargetOrg | null => {
  const idRaw = localStorage.getItem(TARGET_ORG_ID_KEY)
  if (!idRaw) {
    return null
  }
  const orgId = Number(idRaw)
  if (Number.isNaN(orgId) || orgId <= 0) {
    localStorage.removeItem(TARGET_ORG_ID_KEY)
    localStorage.removeItem(TARGET_ORG_NAME_KEY)
    return null
  }
  const orgName = localStorage.getItem(TARGET_ORG_NAME_KEY) || undefined
  return { orgId, orgName }
}

export const clearTargetOrgStorage = () => {
  localStorage.removeItem(TARGET_ORG_ID_KEY)
  localStorage.removeItem(TARGET_ORG_NAME_KEY)
}

