package uniChess;

import java.util.ArrayList;
import java.util.List;

/**
*	An object representing a chess piece. Pieces hold histories of locations they have been moved to.  
*/
public class Piece {

	/**	The Color of the Piece*/
	public Game.Color color;
	
	/**	The unicode representation of the Piece*/
	public String unicodeSymbol;

	/**	The plaintext representation of the piece*/
	public String symbol;

	/**	
	*The {@code PieceType} of this Piece
	*/
	public Game.PieceType type;
	
	/** The material value of this piece.*/
	public double value;

	/**	The amount of times this piece has moved*/
	public int moves = 0;

	/** Indicates whether this piece is under attack*/
	public Move attackingMove = null;

	/** Whether or not this pawn can currently be captured via en passant**/
	public boolean passable = false;

	/**
	 * Creates new Piece from single char where uppercase is mapped to black pieces and lowercase
	 * is mapped to white
	 * @param character
	 */
	public static Piece synthesizePiece(char character){
		Game.Color color = Game.Color.WHITE;
		if (Character.isUpperCase(character))
			color = Game.Color.BLACK;
		switch (Character.toLowerCase(character)) {
			case 'p':
				return new Piece(color, Game.PieceType.PAWN);
			case 'r':
				return new Piece(color, Game.PieceType.ROOK);
			case 'n':
				return new Piece(color, Game.PieceType.KNIGHT);
			case 'b':
				return new Piece(color, Game.PieceType.BISHOP);
			case 'q':
				return new Piece(color, Game.PieceType.QUEEN);
			case 'k':
				return new Piece(color, Game.PieceType.KING);
			default:
				return null;
		}
	}

	public Piece(Piece other){
		this(other.color, other.type);
		this.moves = other.moves;
		this.passable = other.passable;
	}

	public Piece(Game.Color c, Game.PieceType type){
		this.type = type;
		this.color = c;

		int[] unicodeChars;

		if (color.equals(Game.Color.BLACK) || Game.useDarkChars)
			unicodeChars = new int[]{9823,9820,9822,9821,9819,9818};
		else unicodeChars = new int[]{9817,9814,9816,9815,9813,9812};

		switch(type){
			case PAWN:
				this.value = 2.0;
				this.symbol = "P";
				this.unicodeSymbol = new String(Character.toChars(unicodeChars[0])); 
				break;
			case ROOK:
				this.value = 6.0;
				this.symbol = "R";
				this.unicodeSymbol = new String(Character.toChars(unicodeChars[1]));
				
				break;
			case KNIGHT:
				this.value = 4.0;
				this.symbol = "N";
				this.unicodeSymbol = new String(Character.toChars(unicodeChars[2]));
				
				break;
			case BISHOP:
				this.value = 4.0;
				this.symbol = "B";
				this.unicodeSymbol = new String(Character.toChars(unicodeChars[3]));
				break;
			case QUEEN:
				this.value = 10.0;
				this.symbol = "Q";
				this.unicodeSymbol = new String(Character.toChars(unicodeChars[4]));
				
				break;
			case KING:	
				this.value = 15.0;
				this.symbol = "K";
				this.unicodeSymbol = new String(Character.toChars(unicodeChars[5]));
				
				break;
		}
		if (c.equals(Game.Color.WHITE))
			this.symbol = this.symbol.toLowerCase();
	}

	/**
	*	Determines whether or not the piece is of Game.PieceType t
	*
	*	@param t The type to check
	*	@return Whether the piece has a type of t
	*/
	public boolean ofType(Game.PieceType t){
		return type.equals(t);
	}

	/**
	*	Determines whether or not the symbol associated with the piece
	*	is equal to a given character
	*
	*	@param s The character to check
	*	@return Whether the piece has a symbolic representation of s
	*/
	public boolean ofType(String s){
		return (symbol.equalsIgnoreCase(s) || unicodeSymbol.equals(s));
	}

	/**
	*	@return The symbol according to the Game unicode setting
	*/
	public String getSymbol(){
		return getSymbol(Game.unicode);
	}

	/**
	*	Returns one of the symbols associated with this piece for representation.
	*
	*	@param unicode Whether to get the unicode or the text representation
	*	@return The symbol according to the specification
	*/
	public String getSymbol(boolean unicode){
		if (unicode)
			return unicodeSymbol;
		else return symbol;
	}

	@Override
	public String toString(){
		return ((Game.unicode)?unicodeSymbol:symbol);
	}
}