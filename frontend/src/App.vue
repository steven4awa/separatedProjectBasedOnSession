<script setup>
import { RouterLink, RouterView } from 'vue-router'
import HelloWorld from './components/HelloWorld.vue'
import {get} from "@/net/index.ts"
import {ElMessage} from "element-plus";
import {userStore} from "@/stores";
import router from "@/router";

const store = userStore()
if(store.auth.user === null){
  get("api/user/me", (message)=>{

    // ElMessage.success(message)
    store.auth.user = message;
    console.log(store.auth.user)
    router.push("/index")
  }, ()=>{
    store.auth.user = null
    // ElMessage.warning(message)
  }, (message)=>{
    ElMessage.warning(message)
  })
}

</script>

<template>

    <header>
      <div class="wrapper">
        <HelloWorld msg="This is Main Page" />
        <nav>
          <RouterLink to="/">Login Page</RouterLink>
          <RouterLink to="/index">Index</RouterLink>
        </nav>
      </div>
    </header>

  <RouterView v-slot="{ Component }">
    <Transition
        enter-active-class="animate__animated animate__fadeIn animate__faster "
        leave-active-class="animate__animated animate__fadeOut animate__faster "
        mode="out-in"
    >
      <component :is="Component" />
    </Transition>
  </RouterView>


</template>

<style scoped>
header {
  line-height: 1.5;
  max-height: 100vh;
}

nav {
  width: 100%;
  font-size: 12px;
  text-align: center;
  margin-top: 2rem;
}


nav a {
  display: inline-block;
  padding: 0 1rem;
  border-left: 1px solid var(--color-border);
}

nav a:first-of-type {
  border: 0;
}

@media (min-width: 1024px) {
  header {
    display: flex;
    place-items: center;
    padding-right: calc(var(--section-gap) / 2);
  }

  header .wrapper {
    display: flex;
    place-items: flex-start;
    flex-wrap: wrap;
  }

  nav {
    text-align: left;
    margin-left: -1rem;
    font-size: 1rem;

    padding: 1rem 0;
    margin-top: 1rem;
  }
}
</style>
