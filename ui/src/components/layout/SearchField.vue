<template>
    <b-form-input
        :label="$t('search')"
        @input="onInput"
        v-model="search"
        :placeholder="$t('search')"
    />
</template>
<script>
    import {debounce} from "throttle-debounce";
    export default {
        created() {
            if (this.$route.query.q) {
                this.search = this.$route.query.q;
            }
            this.searchDebounce = debounce(300, () => {
                this.$emit("search", this.search);

                if (this.router) {
                    const query = {...this.$route.query, q: this.search, page: 1};
                    if (!this.search) {
                        delete query.q;
                    }
                    this.$router.push({query});
                }
            });
        },
        props: {
            router: {
                type: Boolean,
                default: true
            }
        },
        data() {
            return {
                search: ""
            };
        },
        methods: {
            onInput() {
                this.searchDebounce();
            },
        },
        destroyed() {
            this.searchDebounce.cancel();
        }
    };
</script>
