<template>
  <NavBar v-if="showNav" />
  <main :class="showNav ? 'pt-14' : ''">
    <router-view />
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from '@/components/NavBar.vue'

const route   = useRoute()
const showNav = computed(() => !['login','register'].includes(route.name))
</script>
