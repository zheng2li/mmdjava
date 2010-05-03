package jp.nyatla.nymmd;
/*import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;*/
import java.util.*;
import jp.nyatla.nymmd.types.*;
/**
 * Wrapper class for IMmdPmdRender to allow for multiple instances
 * @author shadofx
 */
public class MmdPmdRenderMult implements IMmdPmdRender
{
	private Set<IMmdPmdRender> _renders;
	IMmdPmdRender _current;
	/**
	 * Constructor.
	 * There is a set of IMmdPmdRender and a "current" Render that is null at construction 
	 */
	public MmdPmdRenderMult()
	{
		this._renders = new HashSet<IMmdPmdRender>();
		return;
	}
	/**
	 * Adds IMmdPmdRender item to the Set and sets item as "current".
	 * @param item
	 * @return
	 */
	public boolean setRender(IMmdPmdRender item)
	{
		if(_renders.contains(item))
		{
			_current = item;
			return false;
		}
		_renders.add(item);
		return true;
	}
	/**
	 * Runs dispose() on the current Render
	 */
	public void dispose()
	{
		_current.dispose();
	}
	/**
	 * Runs dispose() on all Renders in the Set
	 */
	public void disposeAll()
	{
		for(IMmdPmdRender item:_renders)
		{
			item.dispose();
		}
	}
	/**
	 * Sets the PMD on the "current" Render
	 * Should not be used: the PMD should already be there.
	 */
	public void setPmd(MmdPmdModel i_pmd, IMmdDataIo i_io) throws MmdException
	{
		_current.setPmd(i_pmd,i_io);
	}
	/**
	 * Updates Skinning on "current"
	 * @param i_skinning_mat
	 */
	public void updateSkinning(MmdMatrix[] i_skinning_mat)
	{
		_current.updateSkinning(i_skinning_mat);
	}
	/**
	 * Updates Skinning on all Renders in Set
	 * @param i_skinning_mat
	 */
	public void updateSkinningAll(MmdMatrix[] i_skinning_mat)
	{
		for(IMmdPmdRender item:_renders)
		{
			item.updateSkinning(i_skinning_mat);
		}
	}
	/**
	 * Renders "current" Render
	 */
	public void render()
	{
		_current.render();
	}
	/**
	 * Renders all Renders in Set
	 */
	public void renderAll()
	{
		for(IMmdPmdRender item:_renders)
		{
			item.render();
		}
	}
	
}