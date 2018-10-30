<template>
  <div class="tree-node">
      <div
        class="tree-node-root"
        ref="root-node"
      >
        <svg
          :class="['tree-edge', `tree-edge-to-${edgeDirection}`]"
          height="4.4rem"
          v-if="!isRoot"
          :width="edgeEnd"
        >
          <line
            x1="0" y1="0" x2="100%" y2="100%"
            class="tree-edge-line"
            v-if="edgeDirection === Direction.LEFT"
          />
          <line
            x1="100%" y1="0" x2="0" y2="100%"
            class="tree-edge-line"
            v-else-if="edgeDirection === Direction.RIGHT"
          />
          <line
            x1="1" y1="0" x2="1" y2="100%"
            class="tree-edge-line"
            v-else
          />
        </svg>
        <div
          :class="[
            'tree-node-root-value',
            `tree-node-root-value-${ children.length ? 'inner' : 'leaf' }`
          ]"
          v-text="value"></div>
        <div
          :class="[
            'tree-node-root-secondary-value',
            {
              'tree-node-root-secondary-value-activated': isSecondaryValueShown
            }
          ]"
          :title="secondaryValue"
          v-text="roundedSecondaryValue"></div>
      </div>
      <div class="tree-node-children">
        <tree-node
          class="tree-node-child"
          v-for="(child, i) in children"
          :jsonData="child"
          :key="`child-${i}`"
          :ref="`child-${i}`"
          :edgeEnd="childrenDistances[i] ? childrenDistances[i][0] : 0"
          :edgeDirection="childrenDistances[i] ? childrenDistances[i][1] : Direction.CENTER"
          :isRoot="false"
        />
      </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import utils from '../utils';
enum Direction {
  LEFT = 'left',
  CENTER = 'center',
  RIGHT = 'right',
}

const TreeNodeProps = Vue.extend({
  props: {
    jsonData: Object,
    isRoot: Boolean,
    edgeEnd: Number,
    edgeDirection: String,
  },
});

@Component({
  name: 'tree-node',
})
export default class TreeNode extends TreeNodeProps {
  public value = '';
  public secondaryValue: number = 0;
  public roundedSecondaryValue: number = 0;
  public children = this.jsonData.children ? this.jsonData.children : [];
  public isMounted = false;
  public childrenDistances = [];
  public Direction = Direction;
  public isSecondaryValueShown = false;
  public created() {
    this.secondaryValue = this.jsonData.secondaryValue;
    const roundedSecondaryValue: number =
      Math.round(this.secondaryValue * 100) / 100;
    this.roundedSecondaryValue = roundedSecondaryValue;

    const jsonValue = this.jsonData.value;
    if (utils.isFloat(jsonValue)) {
      const roundedValue: number = Math.round(this.jsonData.value * 100) / 100;
      this.value = roundedValue.toString();
    } else {
      this.value = jsonValue;
    }
  }
  public mounted() {
    this.isMounted = true;
    this.childrenDistances = this.children
      .map((x: object, i: number) => i)
      .map((i: number) => {
        const distance = this.computeDistance(i);
        const direction =
          distance < 0
            ? Direction.LEFT
            : distance > 0
              ? Direction.RIGHT
              : Direction.CENTER;
        return [Math.abs(distance), direction];
      });
  }
  public async showSecondaryValue() {
    for (const index in this.children) {
      if (this.children.hasOwnProperty(index)) {
        const childAsArray = this.$refs[`child-${index}`];
        if (childAsArray instanceof Array && childAsArray.length === 1) {
          const child: any = childAsArray[0];
          if (child.hasOwnProperty('showSecondaryValue')) {
            await child.showSecondaryValue();
          }
        }
      }
    }

    await utils.timeout(150);
    this.isSecondaryValueShown = true;
  }
  private computeDistance(index: number): number {
    if (!this.isMounted) {
      return 0;
    }
    const rootNode = this.$refs['root-node'];
    if (rootNode instanceof Element) {
      const childNode = this.$refs[`child-${index}`];
      if (childNode instanceof Element) {
        return utils.getDistanceBetweenElements(rootNode, childNode);
      } else {
        if (childNode instanceof Array) {
          const firstInChildNode = childNode[0];
          if (firstInChildNode instanceof TreeNode) {
            const tragetNode = firstInChildNode.$refs['root-node'];
            if (tragetNode instanceof Element) {
              return utils.getDistanceBetweenElements(rootNode, tragetNode);
            } else {
              return 0;
            }
          } else if (firstInChildNode instanceof Element) {
            return utils.getDistanceBetweenElements(rootNode, firstInChildNode);
          } else {
            return 0;
          }
        } else {
          return 0;
        }
      }
    } else {
      return 0;
    }
  }
}
</script>