<template>
  <div class="sidebar-brand" :class="{ collapse }">
    <router-link class="brand-link" to="/">
      <span class="brand-mark">CM</span>
      <span v-if="!collapse" class="brand-copy">
        <strong>创作者监测</strong>
        <small>Creator Monitoring</small>
      </span>
    </router-link>
  </div>
</template>

<script setup lang="ts">
defineProps<{ collapse: boolean }>();
</script>

<style scoped>
.sidebar-brand {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  background: #111;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-brand .brand-link {
  min-width: 0;
  width: 100%;
  height: 64px;
  display: flex !important;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
  box-sizing: border-box;
}

.brand-mark {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: #fff;
  background: linear-gradient(135deg, #fe2c55 0%, #111 52%, #25f4ee 100%);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.28);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.brand-copy {
  min-width: 0;
  line-height: 1.2;
}

.brand-copy strong,
.brand-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-copy strong {
  color: #fff;
  font-size: 15px;
  line-height: 20px;
  font-weight: 700;
}

.brand-copy small {
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.46);
  font-size: 10px;
}

.collapse {
  justify-content: center;
  padding: 0;
}

.collapse .brand-link {
  width: 54px;
  justify-content: center;
  gap: 0;
}

.collapse .brand-mark {
  width: 32px;
  height: 32px;
  flex-basis: 32px;
  border-radius: 8px;
  font-size: 12px;
}
</style>
