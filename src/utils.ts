function getCaretPosition(ctrl: any) {
  return ctrl.selectionStart;
}

function setCaretPosition(ctrl: any, pos: number) {
  if (ctrl.setSelectionRange) {
    ctrl.focus();
    ctrl.setSelectionRange(pos, pos);
  } else if (ctrl.createTextRange) {
    const range = ctrl.createTextRange();
    range.collapse(true);
    range.moveEnd('character', pos);
    range.moveStart('character', pos);
    range.select();
  }
}

const getPositionAtCenter = (element: Element) => {
  const data = element.getBoundingClientRect();
  return {
    x: data.left + data.width / 2,
    y: data.top + data.height / 2,
  };
};

const getDistanceBetweenElements = (a: Element, b: Element) => {
  const aPosition = getPositionAtCenter(a);
  const bPosition = getPositionAtCenter(b);

  return aPosition.x - bPosition.x;
};
const timeout = (ms: number) => new Promise((res) => setTimeout(res, ms));

function isFloat(val: string) {
  return val === parseFloat(val).toString();
}
export default {
  getCaretPosition,
  setCaretPosition,
  getDistanceBetweenElements,
  timeout,
  isFloat,
};
