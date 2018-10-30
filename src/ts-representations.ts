export interface BaseExpression {
  operation: string;
}

const CONSTANTS = new Map().set(3.141592653589793, 'π');
const OPERATIONS = new Map<string, string>()
.set('Add', '+')
.set('Sub', '-')
.set('Mul', '×')
.set('Div', '/')
.set('Pow', '^')
.set('Ln', 'ln')
.set('Factorial', '!')
.set('Cos', 'cos')
.set('Sin', 'Sin')
.set('Tan', 'tan');
export class IdentityExpression implements BaseExpression {
  public operation: string = 'Identity';
  public isReference: boolean;
  public value: number | string;
  constructor(isReference: boolean, value: number) {
    this.isReference = isReference;
    this.value = CONSTANTS.get(value) ? CONSTANTS.get(value) : value;
  }
}

// tslint:disable-next-line:max-classes-per-file
export class Expression implements BaseExpression {
  public operation: string;
  public arguments: BaseExpression[];
  constructor(operation: string, args: BaseExpression[]) {
    this.operation = operation;
    this.arguments = args;
  }
}
