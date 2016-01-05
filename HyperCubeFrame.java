import java.awt.Event;
import java.awt.Frame;

@SuppressWarnings("serial")
class HyperCubeFrame extends Frame {
	private HyperCube ghc;

	public HyperCubeFrame(HyperCube hc, String strName) {
		super(strName);
		ghc = hc;
	}

	@SuppressWarnings("static-access")
	public boolean handleEvent(Event evt) {
		if (evt.id == evt.WINDOW_DESTROY) {
			if (ghc.bStandalone)
				System.exit(0);
			else
				ghc.removeFromFrame();
			return true;
		}
		return false;
	}
}
