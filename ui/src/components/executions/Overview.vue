<template>
    <div v-if="execution">
        <b-row class="mb-3">
            <b-col class="crud-align">
                <crud type="CREATE" permission="EXECUTION" :detail="{executionId: execution.id}" />
            </b-col>
            <b-col class="text-right">
                <restart :execution="execution" @follow="forwardEvent('follow', $event)" />
                <kill :execution="execution" />
                <status :status="execution.state.current" />
            </b-col>
        </b-row>
        <b-table :responsive="true" striped hover :items="items" class="mb-0">
            <template #cell(value)="row">
                <router-link
                    v-if="row.item.link"
                    :to="{name: 'executions/update', params: row.item.link}"
                >
                    {{ row.item.value }}
                </router-link>
                <span v-else-if="row.item.date">
                    <date-ago :date="row.item.value" />
                </span>
                <span v-else-if="row.item.duration">
                    <duration :histories="row.item.value" />
                </span>
                <span v-else>
                    <span v-if="row.item.key === $t('revision')">
                        <router-link
                            :to="{name: 'flows/update', params: {id: $route.params.flowId, namespace: $route.params.namespace, tab: 'revisions'}, query: {revisionRight: row.item.value}}"
                        >{{ row.item.value }}</router-link>
                    </span>
                    <span v-else>{{ row.item.value }}</span>
                </span>
            </template>
        </b-table>


        <div v-if="execution.trigger" class="mt-4">
            <h5>{{ $t('trigger') }}</h5>
            <vars :execution="execution" :data="execution.trigger" />
        </div>

        <div v-if="execution.inputs" class="mt-4">
            <h5>{{ $t('inputs') }}</h5>
            <vars :execution="execution" :data="inputs" />
        </div>

        <div v-if="execution.variables" class="mt-4">
            <h5>{{ $t('variables') }}</h5>
            <vars :execution="execution" :data="execution.variables" />
        </div>
    </div>
</template>
<script>
    import {mapState} from "vuex";
    import Status from "../Status";
    import Vars from "./Vars";
    import Restart from "./Restart";
    import Kill from "./Kill";
    import State from "../../utils/state";
    import DateAgo from "../layout/DateAgo";
    import Crud from "override/components/auth/Crud";
    import Duration from "../layout/Duration";


    export default {
        components: {
            Duration,
            Status,
            Restart,
            Vars,
            Kill,
            DateAgo,
            Crud
        },
        methods: {
            forwardEvent(type, event) {
                this.$emit(type, event);
            },
            restart() {
                this.$emit("follow");
            },
            stop() {
                if (!this.execution || State.isRunning(this.execution.state.current)) {
                    return new Date().toISOString(true)
                } else {
                    return this.execution.state.histories[
                        this.execution.state.histories.length - 1
                    ].date;
                }
            }
        },
        watch: {
            $route() {
                if (this.execution.id !== this.$route.params.id) {
                    this.$store.dispatch(
                        "execution/loadExecution",
                        this.$route.params
                    );
                }
            }
        },
        computed: {
            ...mapState("execution", ["execution"]),
            items() {
                if (!this.execution) {
                    return []
                }
                const stepCount = this.execution.taskRunList
                    ? this.execution.taskRunList.length
                    : 0;

                let ret = [
                    {key: this.$t("namespace"), value: this.execution.namespace},
                    {key: this.$t("flow"), value: this.execution.flowId},
                    {
                        key: this.$t("revision"),
                        value: this.execution.flowRevision
                    },
                    {key: this.$t("created date"), value: this.execution.state.histories[0].date, date: true},
                    {key: this.$t("updated date"), value: this.stop(), date: true},
                    {key: this.$t("duration"), value: this.execution.state.histories, duration: true},
                    {key: this.$t("steps"), value: stepCount}
                ];


                if (this.execution.parentId) {
                    ret.push({
                        key: this.$t("parent execution"),
                        value: this.execution.parentId,
                        link: {
                            flowId: this.execution.flowId,
                            id: this.execution.parentId,
                            namespace: this.execution.namespace
                        }
                    });
                }

                return ret;
            },
            inputs() {
                const inputs = {};
                for (const key in this.execution.inputs) {
                    inputs[key] = this.execution.inputs[key];
                }

                return inputs;
            }
        },
    };
</script>
<style scoped lang="scss">
::v-deep thead {
    display: none;
}


.crud-align {
    display: flex;
    align-items: center;
}
</style>
