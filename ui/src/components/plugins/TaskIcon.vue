<template>
    <div class="wrapper" v-if="icon" :class="classes" :id="uuid">
        <div class="icon" :style="styles" :alt="cls" />
        <div v-if="!onlyIcon" class="hover">
            {{ name }}
        </div>
        <b-tooltip :target="uuid" placement="bottom">
            <span>
                {{ tooltipStart }}
            </span>
            <code>
                {{ tooltipEnd }}
            </code>
        </b-tooltip>
    </div>
</template>

<script>
    import {mapState} from "vuex";
    import Utils from "../../utils/utils";

    export default {
        props: {
            cls: {
                type: String,
                required: true
            },
            onlyIcon: {
                type: Boolean,
                default: false
            }
        },
        data() {
            return {
                uuid: Utils.uid()
            }
        },
        components: {
        },
        computed: {
            ...mapState("plugin", ["icons"]),
            name() {
                return this.icon ? this.icon.name : this.cls;
            },
            classes() {
                return {
                    "flowable": this.icon ? this.icon.flowable : false,
                    "only-icon": this.onlyIcon
                }
            },
            styles() {
                return {
                    backgroundImage: `url(data:image/svg+xml;base64,${this.imageBase64})`
                }
            },
            tooltipStart() {
                return this.name ? this.cls.replace("." + this.name, ".") : this.cls;
            },
            tooltipEnd() {
                return this.name ;
            },

            imageBase64() {
                const icon = this.icon ? this.icon.icon : undefined;
                return icon ? icon : Buffer.from("<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                    "xmlns:xlink=\"http://www.w3.org/1999/xlink\" aria-hidden=\"true\" " +
                    "focusable=\"false\" width=\"0.75em\" height=\"1em\" style=\"-ms-transform: " +
                    "rotate(360deg); -webkit-transform: rotate(360deg); transform: rotate(360deg);\" " +
                    "preserveAspectRatio=\"xMidYMid meet\" viewBox=\"0 0 384 512\">" +
                    "<path d=\"M288 32H0v448h384V128l-96-96zm64 416H32V64h224l96 96v288z\" fill=\"#0D1523FF\"/>" +
                    "</svg>", "utf8").toString("base64");
            },
            icon() {
                return (this.icons || {})[this.cls]
            }
        }
    };
</script>

<style lang="scss" scoped>
    @use "sass:math";
    @import "../../styles/_variable.scss";
    div.wrapper {
        height: 100%;
        width: 100%;
        font-size: $font-size-xs;

        > .icon {
            margin: math.div($btn-padding-x-sm, 2);
            height: calc(100% - 25px);
            display: block;
            background-size: contain;
            background-repeat: no-repeat;
            background-position: top center;
        }

        > .hover {
            position: absolute;
            background: var(--indigo-lighten-15);
            border-top: 1px solid $indigo;
            color: $white;
            font-size: 70%;
            text-overflow: ellipsis;
            overflow: hidden;
            max-width: 100%;
            white-space: nowrap;
            width: 100%;
            bottom: 0;
            text-align: center;
            padding: math.div($btn-padding-y-sm, 2) 1px ;
        }

        &.flowable {
            > .hover {
                background: var(--pink-darken-15);
            }
        }

        &.only-icon {
            > .icon {
                margin: 0;
                margin-top: 2px;
                height: 100%;
                vertical-align: center;
            }
        }
    }

</style>
