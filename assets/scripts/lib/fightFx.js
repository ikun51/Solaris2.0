
exports.chainLightningFade = new Effect(25, 500, e => {
	if (!(e.data instanceof Position)) return;
	let p = e.data;
	let tx = p.getX(), ty = p.getY(), dst = Mathf.dst(e.x, e.y, tx, ty);
	Tmp.v1.set(p).sub(e.x, e.y).nor();

	let normx = Tmp.v1.x, normy = Tmp.v1.y, range = e.rotation;
	let links = Mathf.ceil(dst / range), spacing = dst / links;

	Lines.stroke(2.5 * Mathf.curve(e.fout(), 0, 0.7), e.color);

	Lines.beginLine();
	Lines.linePoint(e.x, e.y);
	Fx.rand.setSeed(e.id);

	let fin = Mathf.curve(e.fin(), 0, 0.5);
	for (let i = 0; i < links * fin; i++) {
		let nx, ny;
		if (i == links - 1) {
			nx = tx;
			ny = ty;
		} else {
			let len = (i + 1) * spacing;
			Tmp.v1.setToRandomDirection(Fx.rand).scl(range / 2);
			nx = e.x + normx * len + Tmp.v1.x;
			ny = e.y + normy * len + Tmp.v1.y;
		}
		Lines.linePoint(nx, ny);
	}
	Lines.endLine();
})
exports.chainLightningFade.followParent = true;

exports.chainLightningFade2 = new Effect(20, 500, e => {
	if (!(e.data instanceof Position)) return;
	let p = e.data;
	let tx = e.x, ty = e.y, dst = Mathf.dst(p.getX(), p.getY(), tx, ty);
	Tmp.v1.set(e.x, e.y).sub(p).nor();

	let normx = Tmp.v1.x, normy = Tmp.v1.y, range = e.rotation;
	let links = Mathf.ceil(dst / range), spacing = dst / links;

	// 结合旧代码：瞬间画满整条闪电，白色闪光，快速淡出（不再渐进生长/回缩）
	Lines.stroke(2.5 * e.fout());
	Draw.color(Color.white, e.color, e.fin());

	Lines.beginLine();
	Lines.linePoint(p);
	Fx.rand.setSeed(e.id);

	for (let i = 0; i < links; i++) {
		let nx, ny;
		if (i == links - 1) {
			nx = tx;
			ny = ty;
		} else {
			let len = (i + 1) * spacing;
			Tmp.v1.setToRandomDirection(Fx.rand).scl(range / 2);
			nx = p.getX() + normx * len + Tmp.v1.x;
			ny = p.getY() + normy * len + Tmp.v1.y;
		}
		Lines.linePoint(nx, ny);
	}
	Lines.endLine();
})
exports.chainLightningFade2.followParent = false;

exports.chainLightningFadeReversed = new Effect(25, 500, e => {
	if (!(e.data instanceof Position)) return;
	let p = e.data;
	let tx = e.x, ty = e.y, dst = Mathf.dst(p.getX(), p.getY(), tx, ty);
	Tmp.v1.set(e.x, e.y).sub(p).nor();

	let normx = Tmp.v1.x, normy = Tmp.v1.y, range = e.rotation;
	let links = Mathf.ceil(dst / range), spacing = dst / links;

	Lines.stroke(2.5 * Mathf.curve(e.fout(), 0, 0.7), e.color);

	Lines.beginLine();
	Lines.linePoint(p);
	Fx.rand.setSeed(e.id);

	let fin = Mathf.curve(e.fin(), 0, 0.5);
	for (let i = 0; i < links * fin; i++) {
		let nx, ny;
		if (i == links - 1) {
			nx = tx;
			ny = ty;
		} else {
			let len = (i + 1) * spacing;
			Tmp.v1.setToRandomDirection(Fx.rand).scl(range / 2);
			nx = p.getX() + normx * len + Tmp.v1.x;
			ny = p.getY() + normy * len + Tmp.v1.y;
		}
		Lines.linePoint(nx, ny);
	}
	Lines.endLine();
})
exports.chainLightningFadeReversed.followParent = false;

exports.smallRectBurst = new Effect(18, 60, e => {
	Draw.color(e.color);
	Draw.alpha(e.fout());
	for (let i = 0; i < 12; i++) {
		let rot = Mathf.randomSeed(e.id * 7 + i, 360);
		let dist = Mathf.randomSeed(e.id + i, 42) * e.fin();
		Tmp.v1.trns(rot, dist);
		Fill.rect(e.x + Tmp.v1.x, e.y + Tmp.v1.y, 3, 1, rot);
	}
})
exports.smallRectBurst.followParent = false;

