<template>
  <div :class="['app', `app-${mode}-mode`]">
    <transition name="fade">
        <div class="calculator-view" v-if="mode === 'calculator'">
          <div class="result">
            <input
              type="text"
              :class="[
                'expression-input',
                syntaxError ? 'expression-input-error' : '',
              ]"
              ref="expression-input"
              v-model="expressionAsString"
              @input="expressionChanged()" />
          </div>
          <div class="functions">
            <div class="semi-bold-button factorial" @click="addSymbol('!')">x!</div>
            <div class="semi-bold-button pow2" @click="addSymbol('^2')">x<span class="upper-text">2</span></div>
            <div class="semi-bold-button sqrt" @click="addSymbol('√')">√</div>
            <div class="semi-bold-button pow" @click="addSymbol('^')">x<span class="upper-text">y</span></div>
            <div class="semi-bold-button sin" @click="addSymbol('sin')">sin</div>
            <div class="semi-bold-button cos" @click="addSymbol('cos')">cos</div>
            <div class="semi-bold-button tan" @click="addSymbol('tan')">tan</div>
            <div class="semi-bold-button ln" @click="addSymbol('ln')">ln</div>
            <div class="semi-bold-button open-brackets" @click="addSymbol('(')">(</div>
            <div class="semi-bold-button close-brackets" @click="addSymbol(')')">)</div>
          </div>
          <div class="digits">
            <div class="digit-button digit" @click="addSymbol('1')">1</div>
            <div class="digit-button digit" @click="addSymbol('2')">2</div>
            <div class="digit-button digit" @click="addSymbol('3')">3</div>
            <div class="digit-button digit" @click="addSymbol('4')">4</div>
            <div class="digit-button digit" @click="addSymbol('5')">5</div>
            <div class="digit-button digit" @click="addSymbol('6')">6</div>
            <div class="digit-button digit" @click="addSymbol('7')">7</div>
            <div class="digit-button digit" @click="addSymbol('8')">8</div>
            <div class="digit-button digit" @click="addSymbol('8')">9</div>
            <div class="digit-button digit" @click="addSymbol('0')">0</div>
            <div class="digit-button dot" @click="addSymbol('.')">.</div>
            <div class="digit-button ans" @click="addSymbol('Ans')">Ans</div>
          </div>
          <div class="operations">
            <div class="operation bold-button del" @click="deleteSymbol()">Del</div>
            <div class="operation bold-button ac" @click="clear()">AC</div>
            <div class="operation add" @click="addSymbol('+')">+</div>
            <div class="operation sub" @click="addSymbol('-')">-</div>
            <div class="operation mul" @click="addSymbol('×')">×</div>
            <div class="operation div" @click="addSymbol('÷')">÷</div>
            <div class="operation bold-button equal" @click="calc()">=</div>
          </div>
        </div>
    </transition>
    <transition name="fade">
      <div class="tree-view" v-if="mode === 'tree-view'">
        <div class="expression">
          <span
            class="expression-as-string"
          >
            <span v-text="expressionAsString"></span>
            <transition name="fade">
              <span
                class="expression-result"
                v-text="`=${result}`"
                v-if="result.length"></span>
            </transition>
          </span>
          <button
            class="global-button evaluate-button"
            @click="evaluate()"
          >
            Evaluate
          </button>
        </div>
        <TreeNode
          :jsonData="evaluatedTree"
          :isRoot="true"
          ref="tree-view"
          />
      </div>
    </transition>
    <transition name="fade">
      <button
        class="global-button back-button"
        @click="back()"
        v-if="mode !== 'calculator'">
      </button>
    </transition>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import './scala/ExpressionsEvaluater/main/target/scala-2.12/main-fastopt';
import utils from './utils';
import TreeNode from './components/TreeNode.vue';
declare const TreeBuilderAndEvaluator: any;

@Component({ components: { TreeNode } })
export default class App extends Vue {
  public expressionAsString: string = '3+(4^2+5^cos(π÷3)÷(4+2))×6';
  public evaluatedTree: object = {};

  public mode: string = 'calculator';
  public focuesdSubExpression = null;

  public lastAnswer = 0;
  public result = '';
  public syntaxError = false;

  private calc() {
    const expressionAsString = this.expressionAsString
      .replace('/', '÷')
      .replace('*', '×')
      .replace('Ans', this.lastAnswer.toString());

    try {
      const tree = TreeBuilderAndEvaluator.buildTreeFromString(
        expressionAsString,
      );
      if (tree) {
        this.result = '';
        this.evaluatedTree = TreeBuilderAndEvaluator.evaluateTree(tree);
        this.mode = 'tree-view';
      }
    } catch (err) {
      this.syntaxError = true;
    }
  }

  private addSymbol(s: string) {
    const expressionInput = this.$refs['expression-input'];
    const caretPosition = utils.getCaretPosition(expressionInput);
    this.expressionAsString =
      this.expressionAsString.substr(0, caretPosition) +
      s +
      this.expressionAsString.substr(caretPosition);
    this.$nextTick(() =>
      utils.setCaretPosition(expressionInput, caretPosition + s.length),
    );
    this.expressionChanged();
  }

  private deleteSymbol() {
    const expressionInput = this.$refs['expression-input'];
    const caretPosition = utils.getCaretPosition(expressionInput);
    this.expressionAsString =
      this.expressionAsString.substr(0, caretPosition - 1) +
      this.expressionAsString.substr(caretPosition);
    this.$nextTick(() =>
      utils.setCaretPosition(expressionInput, caretPosition - 1),
    );
    this.expressionChanged();
  }

  private clear() {
    this.expressionAsString = '';
    this.expressionChanged();
  }

  private expressionChanged() {
    this.syntaxError = false;
  }

  private async evaluate() {
    const treeView = this.$refs['tree-view'];
    if (treeView instanceof TreeNode) {
      await treeView.showSecondaryValue();

      const resultAsString: string = String(
        treeView.jsonData.secondaryValue,
      ).toString();
      const roundedResult: number =
        Math.round(parseFloat(resultAsString) * 10000) / 10000;

      this.result = roundedResult.toString();

      const result = treeView.jsonData.secondaryValue;
      this.lastAnswer = result;
    }
  }

  private back() {
    this.mode = 'calculator';
    this.clear();
  }
}
</script>

<style lang="sass">
@import 'assets/_app'
</style>